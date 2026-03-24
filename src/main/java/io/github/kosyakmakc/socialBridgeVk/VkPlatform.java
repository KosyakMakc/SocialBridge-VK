package io.github.kosyakmakc.socialBridgeVk;

import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.ITransaction;
import io.github.kosyakmakc.socialBridge.Modules.ISocialModule;
import io.github.kosyakmakc.socialBridge.Modules.ITranslationsModule;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialMessage;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeVk.DatabaseTables.VkUserTable;
import io.github.kosyakmakc.socialBridgeVk.Utils.CacheContainer;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;
import io.github.kosyakmakc.socialBridgeVk.Utils.TranslationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import com.j256.ormlite.table.TableUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Forward;
import com.vk.api.sdk.objects.users.Fields;
import com.vk.api.sdk.objects.users.UserFull;
import com.vk.api.sdk.objects.users.responses.GetResponse;

public class VkPlatform implements ISocialPlatform {
    public static final String PLATFORM_NAME = "VK";
    public static final UUID PLATFORM_ID = UUID.fromString("5952ca6e-2567-4475-ab96-ad0b5383088b");
    private static final String configurationPath = "social-bridge-vk";
    private static final String configurationPathToken = configurationPath + "_token";
    private static final String configurationPathRetryMax = configurationPath + "_retries-max";
    private static final int defaultRetryMax = 10;
    private static final String configurationPathRetryDelay = configurationPath + "_retries-delay";
    private static final int defaultRetryDelay = 2;

    private final CacheContainer<VkUser> userCaching = new CacheContainer<>(500);

    private final Version socialBridgeCompabilityVersion = new Version("0.10.1");
    
    private BotState botState = BotState.Stopped;
    private VkApiClient client;
    private LongPollingHandler longpollHandler;

    private LinkedList<ISocialModule> connectedModules = new LinkedList<>();

    private ISocialBridge bridge;
    private Logger logger;
    private Random random;

    public CompletableFuture<Boolean> startBot() {
        if (botState != BotState.Stopped) {
            return CompletableFuture.completedFuture(false);
        }

        logger.info("VK bot starting...");

        botState = BotState.Starting;
        return getBotToken(null)
        .thenCompose(tokenDefinition -> {
            if (tokenDefinition.isBlank()) {
                logger.info("Token missed, connect to VK canceled");
                botState = BotState.Stopped;
                return CompletableFuture.completedFuture(false);
            }

            var separatorIndex = tokenDefinition.indexOf(":");
            var groupId = Long.parseLong(tokenDefinition.substring(0, separatorIndex));
            var token = tokenDefinition.substring(separatorIndex + 1, tokenDefinition.length());

            var httpClient = new HttpTransportClient();
            client = new VkApiClient(httpClient);
            var groupActor = new GroupActor(groupId, token);

            longpollHandler = new LongPollingHandler(this, client, groupActor);
            longpollHandler.run();
            return CompletableFuture.completedFuture(true);
        })
        .thenCompose(isSuccessStart -> {
            if (isSuccessStart) {
                return withRetries(() -> {
                    try {
                        var groupInfoResponse = client
                            .groups()
                            .getByIdObject(longpollHandler.getActor())
                                .groupId(Long.toString(-getGroupId()))
                            .execute();
                        var groupName = groupInfoResponse.getGroups().getFirst().getScreenName();
                        longpollHandler.setBotName(groupName);
                        random = new Random(Instant.now().getEpochSecond());

                        botState = BotState.Started;
                        logger.info("VK bot connected");
                        return true;
                    } catch (ApiException e) {
                        e.printStackTrace();
                        longpollHandler.stop();
                        client = null;
                        return false;
                    } catch (ClientException e) {
                        e.printStackTrace();
                        longpollHandler.stop();
                        client = null;
                        return false;
                    }
                });
            }
            else {
                return CompletableFuture.completedFuture(isSuccessStart);
            }
        })
        .thenComposeAsync(isSuccessStart -> {
            if (isSuccessStart) {
                return updateCommandSuggestions();
            }
            else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

    public CompletableFuture<Boolean> stopBot() {
        if (botState != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        logger.info("VK bot stopping...");

        botState = BotState.Stopping;

        longpollHandler.stop();
        client = null;
        longpollHandler = null;
        random = null;
        botState = BotState.Stopped;
        logger.info("VK bot stopped");
        return CompletableFuture.completedFuture(true);
    }
    
    public CompletableFuture<Boolean> setupToken(String token, ITransaction transaction) {
        var vkModule = getVkModule();

        var saveConfigTask =  getBridge().getConfigurationService().set(vkModule, configurationPathToken, token, transaction)
                             .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(VkMessageKey.SET_TOKEN_FAILED_CONFIG)));

        var stoppingTask = saveConfigTask
                            .thenCompose(isSuccess -> botState != BotState.Stopped ? stopBot() : CompletableFuture.completedFuture(true))
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(VkMessageKey.SET_TOKEN_FAILED_STOP_BOT)));

        var startingTask = stoppingTask
                            .thenCompose(isSuccess -> startBot())
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(VkMessageKey.SET_TOKEN_FAILED_START_BOT)));

        return startingTask;
    }

    public BotState getBotState() {
        return botState;
    }

    private CompletableFuture<String> getBotToken(ITransaction transaction) {
        var vkModule = getVkModule();
        return getBridge().getConfigurationService().get(vkModule, configurationPathToken, "", transaction);
    }

    public CompletableFuture<Boolean> setupMaxRetries(int retries, ITransaction transaction) {
        var vkModule = getVkModule();
        return getBridge().getConfigurationService().set(vkModule, configurationPathRetryMax, Integer.toString(retries), transaction);
    }

    private CompletableFuture<Integer> getMaxRetry(ITransaction transaction) {
        var vkModule = getVkModule();
        return getBridge().getConfigurationService().get(vkModule, configurationPathRetryMax, "", transaction)
              .thenApply(rawNumber -> {
                  try {
                      return Integer.parseInt(rawNumber);
                  }
                  catch (NumberFormatException err) {
                      return defaultRetryMax;
                  }
              });
    }

    public CompletableFuture<Boolean> setupRetryDelay(Duration delay, ITransaction transaction) {
        var vkModule = getVkModule();
        return getBridge().getConfigurationService().set(vkModule, configurationPathRetryDelay, Long.toString(delay.toSeconds()), transaction);
    }

    private CompletableFuture<Integer> getRetryDelay(ITransaction transaction) {
        var vkModule = getVkModule();
        return getBridge().getConfigurationService().get(vkModule, configurationPathRetryDelay, "", transaction)
              .thenApply(rawNumber -> {
                  try {
                      return Integer.parseInt(rawNumber);
                  }
                  catch (NumberFormatException err) {
                      return defaultRetryDelay;
                  }
              });
    }

    public ISocialBridge getBridge() {
        return bridge;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public UUID getId() {
        return PLATFORM_ID;
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(Identifier channelId, String template, HashMap<String, String> placeholders) {
        var message = BuildTemplateMessage(template, placeholders);

        if (botState == BotState.Started) {
            try {
                var peerId = (long) channelId.value();
                client
                    .messages()
                    .sendDeprecated(longpollHandler.getActor())
                        .peerId(peerId)
                        .message(message)
                        .randomId(random.nextInt())
                    .execute();
                logger.info("vkMessage to channel id=" + peerId + " - " + message);
            }
            catch (ApiException | ClientException err) {
                err.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(Identifier channelId, MessageKey message, String locale, HashMap<String, String> placeholders, ITransaction transaction) {
        return bridge
            .getLocalizationService()
            .getMessage(locale, message, transaction)
            .thenCompose(templateMessage -> sendMessage(channelId, templateMessage, placeholders));
    }

    public CompletableFuture<Boolean> sendReply(ISocialMessage socialMessage, String template, HashMap<String, String> placeholders) {
        if (!(socialMessage instanceof VkSocialMessage vkMessage)) {
            throw new RuntimeException("Social message from another SocialPlatform, please provide messages from this SocialPlatform");
        }

        var message = BuildTemplateMessage(template, placeholders);

        if (botState == BotState.Started) {
            try {
                var peerId = vkMessage.getVkPeerId();
                client
                    .messages()
                    .sendDeprecated(longpollHandler.getActor())
                        .peerId(peerId)
                        .message(message)
                        //.replyTo(vkMessage.getVkMessageId())
                        .forward(new Forward()
                            .setIsReply(true)
                            .setPeerId(peerId)
                            .setConversationMessageIds(List.of(vkMessage.getVkConversationMessageId())))
                        .randomId(random.nextInt())
                    .execute();
                logger.info("vkMessage to channel id=" + peerId + " - " + message);
            }
            catch (ApiException | ClientException err) {
                err.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    public CompletableFuture<Boolean> sendMessage(SocialUser socialUser, String template, HashMap<String, String> placeholders) {
        if (!(socialUser instanceof VkUser vkUser)) {
            throw new RuntimeException("Social message from another SocialPlatform, please provide messages from this SocialPlatform");
        }
        var message = BuildTemplateMessage(template, placeholders);

        if (botState == BotState.Started) {
            try {
                var peerId = vkUser.getUserRecord().getId();
                client
                    .messages()
                    .sendDeprecated(longpollHandler.getActor())
                        .peerId(peerId)
                        .message(message)
                        .randomId(random.nextInt())
                    .execute();
                logger.info("vkMessage to \"" + socialUser.getName() + "\" - " + message);
            }
            catch (ApiException | ClientException err) {
                err.printStackTrace();
                return CompletableFuture.completedFuture(false);
            }
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public Version getCompabilityVersion() {
        return socialBridgeCompabilityVersion;
    }

    private String BuildTemplateMessage(String template, HashMap<String, String> placeholders) {
        var builder = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.newline())
                        .build());

        for (var placeholderKey : placeholders.keySet()) {
            builder.editTags(x -> x.resolver(Placeholder.component(placeholderKey, Component.text(placeholders.get(placeholderKey)))));
        }

        var resolvedComponents = builder.build().deserialize(template);
        return MiniMessage.miniMessage().serialize(resolvedComponents);
    }

    private CompletableFuture<Boolean> withRetries(Callable<Boolean> callable) {
        return CompletableFuture.supplyAsync(() ->  {
            var retryCounter = 0;
            var delay = getRetryDelay(null).join();
            var maxRetries = getMaxRetry(null).join();

            while (retryCounter < maxRetries) {
                try {
                    if (callable.call()) {
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                retryCounter++;
                try {
                    var delaySeconds = (int) Math.pow(delay, retryCounter);
                    Thread.sleep(Duration.ofSeconds(delaySeconds));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                    return false;
                }
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Void> connectModule(ISocialModule module) {
        connectedModules.add(module);
        return updateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<Void> disconnectModule(ISocialModule module) {
        var isRemoved = connectedModules.remove(module);
        if (isRemoved) {
            return updateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    // private static final Pattern TelegramValidCommandToken = Pattern.compile("^[a-z0-9_]{1,32}$");

    private CompletableFuture<Boolean> updateCommandSuggestions() {
        var languages = new HashSet<String>();
        for (var module : connectedModules) {
            if (module instanceof ITranslationsModule moduleWithTranslations) {
                for (var translationSource : moduleWithTranslations.getTranslations()) {
                    languages.add(translationSource.getLanguage());
                }
            }
        }

        @SuppressWarnings("unchecked")
        var commandInfos = connectedModules
            .stream()
            .mapMulti((x, consumer) -> {
                // var matcher = TelegramValidCommandToken.matcher(x.getName());
                // if (!matcher.find()) {
                //     logger.warning("module name '" + x.getName() + "' not valid for telegram suggestion, skips all his command from suggestion. But commands is keep working");
                //     return;
                // }

                x.getSocialCommands().forEach(y -> consumer.accept(new AbstractMap.SimpleImmutableEntry<>(x, y)));
            })
            .map(x -> (Entry<ISocialModule, ISocialCommand>) x)
            .map(pair -> {
                // var finalName = pair.getKey().getName() + '_' + pair.getValue().getLiteral();
                // var matcher = TelegramValidCommandToken.matcher(finalName);
                // if (!matcher.find()) {
                //     logger.warning("telegram command name '" + finalName + "' not valid for telegram suggestion, skips this command from suggestion. But command is keep working");
                //     return null;
                // }

                if (pair.getValue().getDescription() == MessageKey.EMPTY) {
                    return null;
                }

                return pair;
            })
            .filter(x -> x != null)
            .toList();

        return CompletableFuture
            .allOf(languages.stream().map(x -> updateCommandSuggestions(x, commandInfos)).toArray(CompletableFuture[]::new))
            .thenRun(() -> updateCommandSuggestions(null, commandInfos))
            .thenApply(Void -> true);
    }

    private CompletableFuture<Boolean> updateCommandSuggestions(String languageCode, List<Entry<ISocialModule, ISocialCommand>> commands) {
        if (getBotState() != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(false);
        // @SuppressWarnings("unchecked")
        // var tasks = (CompletableFuture<BotCommand>[]) commands
        //     .stream()
        //     .map(pair -> {
        //         var finalName = pair.getKey().getName() + '_' + pair.getValue().getLiteral();

        //         var localizationLanguage = languageCode != null
        //                                     ? languageCode
        //                                     : LocalizationService.defaultLocale;

        //         return bridge.getLocalizationService()
        //             .getMessage(localizationLanguage, pair.getValue().getDescription(), null)
        //             .thenApply(description -> new BotCommand(finalName, description));
        //     })
        //     .toArray(CompletableFuture[]::new);

        // return CompletableFuture
        //     .allOf(tasks)
        //     .thenCompose(Void -> {
        //         var commandsInfo = Arrays
        //             .stream(tasks)
        //             .map(task -> {
        //                 try {
        //                     return task.get();
        //                 } catch (InterruptedException | ExecutionException e) {
        //                     e.printStackTrace();
        //                     return null;
        //                 }
        //             })
        //             .filter(commandInfo -> commandInfo != null)
        //             .toList();

        //         if (commandsInfo.isEmpty()) {
        //             return CompletableFuture.completedFuture(true);
        //         }
        //         else {
        //             var commandQuery = new SetMyCommands(commandsInfo);
        //             commandQuery.setLanguageCode(languageCode);
        //             return this.withRetries(() -> telegramClient.execute(commandQuery));
        //         }
        //     });
    }

    @Override
    public CompletableFuture<Boolean> enable(ISocialBridge socialBridge) {
        this.bridge = socialBridge;
        logger = Logger.getLogger(this.bridge.getLogger().getName() + '.' + VkPlatform.class.getSimpleName());

        var initTask = this.bridge.doTransaction(transaction -> {
            var databaseContext = transaction.getDatabaseContext();

            try {
                TableUtils.createTableIfNotExists(databaseContext.getConnectionSource(), VkUserTable.class);
                var daoSession = databaseContext.registerTable(VkUserTable.class);

                if (daoSession == null) {
                    throw new RuntimeException("Failed to create required database table - " + VkUserTable.class.getSimpleName());
                }

                return CompletableFuture.completedFuture(true);
            } catch (SQLException e) {
                e.printStackTrace();
                this.bridge = null;
                return CompletableFuture.completedFuture(false);
            }
        });

        // starting in background
        initTask.thenCompose(d -> startBot());

        return initTask;
    }

    @Override
    public CompletableFuture<Void> disable() {
        this.bridge = null;
        return stopBot().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<SocialUser> tryGetUser(Identifier id, ITransaction transaction) {
        var cachedUser = userCaching.tryGet(x -> (long) x.getId().value() == (long) id.value());
        if (cachedUser != null) {
            return CompletableFuture.completedFuture(cachedUser);
        }

        return transaction == null
                ? this.bridge.doTransaction(transaction2 -> getUserFromDatabase(id, transaction2))
                : getUserFromDatabase(id, transaction);
    }

    private CompletableFuture<SocialUser> getUserFromDatabase(Identifier id, ITransaction transaction) {
        var databaseContext = transaction.getDatabaseContext();

        var dao = databaseContext.getDaoTable(VkUserTable.class);

        VkUserTable dbUser;
        try {
            dbUser = dao.queryForId((long) id.value());
        } catch (SQLException e) {
            e.printStackTrace();
            dbUser = null;
        }

        if (dbUser == null) {
            return CompletableFuture.completedFuture(null);
        }

        var user = new VkUser(this, dbUser);
        userCaching.checkAndAdd(user);
        return CompletableFuture.completedFuture(user);
    }

    public CompletableFuture<UserFull> getUserFromVk(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GetResponse> answers;
            try {
                answers = client
                        .users()
                        .get(longpollHandler.getActor())
                            .userIds(Long.toString(userId))
                            .fields(List.of(Fields.DOMAIN))
                            .execute();
                return answers.size() == 0 ? null : answers.get(0);
            } catch (ApiException e) {
                e.printStackTrace();
                return null;
            } catch (ClientException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private VkModule getVkModule() {
        if (bridge == null) {
            throw new RuntimeException("VK platform has been disconnected from SocialBridge");
        }

        var module = bridge.getModule(VkModule.class);

        if (module == null) {
            throw new RuntimeException("Required VK module not connected to SocialBridge");
        }

        return module;
    }

    public String getGroupName() {
        return longpollHandler.getBotName();
    }

    public Long getGroupId() {
        return longpollHandler.getActor().getId();
    }

    public Logger getLogger() {
        return logger;
    }
}
