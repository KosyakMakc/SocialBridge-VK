package io.github.kosyakmakc.socialBridgeVk;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Modules.ISocialModule;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.IdentifierType;
import io.github.kosyakmakc.socialBridgeVk.DatabaseTables.VkUserTable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.events.longpoll.GroupLongPollApi;
import com.vk.api.sdk.objects.callback.MessageNew;

public class LongPollingHandler extends GroupLongPollApi {
    private final VkPlatform socialPlatform;
    private final GroupActor actor;

    private String botUsername;

    public LongPollingHandler(VkPlatform vkPlatform, VkApiClient vkClient, GroupActor groupActor) {
        super(vkClient, groupActor, 10);
        this.socialPlatform = vkPlatform;
        this.actor = groupActor;
    }

    @Override
    public void messageNew(Integer groupId, MessageNew update) {
        var messageObject = update.getObject();
        if (messageObject == null) {
            return;
        }
        var message = messageObject.getMessage();
        if (message == null) {
            return;
        }
        var messageText = message.getText();
        
        if (messageText == null || messageText.isBlank()) {
            return;
        }

        CheckAndCacheUser(message.getFromId());

        // Commands handling
        if (tryCommandHandle(update)) {
            return;
        }
        
        // TODO Messages handling in future
    }

    private void CheckAndCacheUser(Long fromId) {
        socialPlatform.tryGetUser(new Identifier(IdentifierType.Long, fromId), null)
            .thenCompose(socialUser -> {
                if (socialUser == null) {
                    return socialPlatform.getUserFromVk(fromId)
                        .thenApply(vkUser -> {
                            var dbUser = new VkUserTable(fromId, vkUser.getDomain(), vkUser.getFirstName(), vkUser.getLastName(), "ru");
                            var socialUser2 = new VkUser(socialPlatform, dbUser);
                            
                            // non-blocking save user in background
                            socialPlatform.getBridge().doTransaction(transaction -> {
                                var databaseContext = transaction.getDatabaseContext();
                                
                                var table = databaseContext.getDaoTable(VkUserTable.class);
                                try {
                                    table.createIfNotExists(dbUser);
                                    return CompletableFuture.completedFuture(true);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    return CompletableFuture.completedFuture(false);
                                }
                            });
                            return socialUser2;
                        });
                }
                return CompletableFuture.completedFuture(socialUser);
            });
    }

    private boolean tryCommandHandle(MessageNew chatEvent) {
        var textMessage = chatEvent.getObject().getMessage().getText();
        if (textMessage.charAt(0) != '/') {
            return false;
        }

        var socialMessage = new VkSocialMessage(socialPlatform, chatEvent.getObject());
        var commandCtx = new VkSocialCommandExecutionContext(socialMessage, botUsername);

        try {
            for (var module : socialPlatform.getBridge().getModules()) {

                if (!(module instanceof ISocialModule socialModule)) {
                    continue;
                }

                if (!commandCtx.getModuleName().equals(module.getName())) {
                    continue;
                }

                for (var socialCommand : socialModule.getSocialCommands()) {
                    if (commandCtx.getCommandLiteral().equals(socialCommand.getLiteral())) {
                        socialCommand.handle(commandCtx);
                        return true;
                    }
                }
            }
        } catch (ArgumentFormatException e) {
            e.logTo(socialPlatform.getBridge().getLogger());
            commandCtx.getSocialMessage().sendReply(e.getMessage(), new HashMap<String, String>());
            return true;
        }
        return false;
    }

    public GroupActor getActor() {
        return actor;
    }

    public void setBotName(String botName) {
        botUsername = botName;
    }

    public String getBotName() {
        return botUsername;
    }
}
