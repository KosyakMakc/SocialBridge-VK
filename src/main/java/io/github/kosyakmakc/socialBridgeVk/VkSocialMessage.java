package io.github.kosyakmakc.socialBridgeVk;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

import com.vk.api.sdk.objects.callback.MessageObject;
import com.vk.api.sdk.objects.messages.ForeignMessage;

import io.github.kosyakmakc.socialBridge.ITransaction;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialAttachment;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialMessage;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.IdentifierType;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeVk.DatabaseTables.VkUserTable;

public class VkSocialMessage implements ISocialMessage {
    private final VkPlatform socialPlatform;
    private final long chatId;
    private final int messageId;
    private final int conversationMessageId;
    private final String messageText;
    private final long authorId;

    private final LinkedList<ISocialAttachment> attachments = new LinkedList<>();
    private CompletableFuture<SocialUser> socialUserLoading;

    public VkSocialMessage(VkPlatform socialPlatform, MessageObject vkMessage) {
        this.socialPlatform = socialPlatform;
        this.chatId = vkMessage.getMessage().getPeerId();
        this.messageText = vkMessage.getMessage().getText();
        this.authorId = vkMessage.getMessage().getFromId();

        this.messageId = vkMessage.getMessage().getId();
        this.conversationMessageId = vkMessage.getMessage().getConversationMessageId();
        
        var replyMessage = vkMessage.getMessage().getReplyMessage();
        if (replyMessage != null) {
            attachments.add(new VkSocialAttachmentReply(socialPlatform, replyMessage));
        }
    }

    public VkSocialMessage(VkPlatform socialPlatform, ForeignMessage vkMessage) {
        this.socialPlatform = socialPlatform;
        this.messageId = vkMessage.getId();
        this.conversationMessageId = vkMessage.getConversationMessageId();
        this.chatId = vkMessage.getId();
        this.messageText = vkMessage.getText();
        this.authorId = vkMessage.getFromId();

        var replyMessage = vkMessage.getReplyMessage();
        if (replyMessage != null) {
            attachments.add(new VkSocialAttachmentReply(socialPlatform, replyMessage));
        }
    }

    @Override
    public Collection<ISocialAttachment> getAttachments() {
        return attachments;
    }

    @Override
    public CompletableFuture<SocialUser> getAuthor() {
        if (socialUserLoading == null) {
            socialUserLoading = loadSocialUser();
        }
        return socialUserLoading;
    }

    @Override
    public Identifier getChannelId() {
        return new Identifier(IdentifierType.Long, chatId);
    }

    public long getVkPeerId() {
        return chatId;
    }

    @Override
    public Identifier getId() {
        return new Identifier(IdentifierType.Integer, messageId);
    }

    public int getVkMessageId() {
        return messageId;
    }

    public int getVkConversationMessageId() {
        return conversationMessageId;
    }

    @Override
    public String getStringMessage() {
        return messageText;
    }

    private CompletableFuture<SocialUser> loadSocialUser() {
        var identifier = new Identifier(IdentifierType.Long, authorId);
        return socialPlatform.tryGetUser(identifier, null)
            .thenCompose(socialUser -> {
                if (socialUser == null) {
                    return socialPlatform.getUserFromVk(authorId)
                        .thenApply(vkUser -> {
                            var dbUser = new VkUserTable(authorId, vkUser.getDomain(), vkUser.getFirstName(), vkUser.getLastName(), "ru");
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
            })
            .thenCompose(socialUser -> {
                var now = Instant.now();
                if (socialUser instanceof VkUser vkUser
                 && vkUser.getUserRecord().getUpdatedAt().toInstant().plus(Duration.ofDays(1)).isBefore(now)) {
                    return socialPlatform
                        .getUserFromVk(authorId)
                        .thenApply(newData -> {
                            var isChanged = vkUser.tryActualize(newData);
                            if (isChanged) {
                                socialPlatform.getLogger().info("vk user info updated (id " + authorId + " - " + vkUser.getName() + ")");
                            }
                            return vkUser;
                        });
                }

                return CompletableFuture.completedFuture(socialUser);
            });
    }

    @Override
    public CompletableFuture<Boolean> sendReply(String messageTemplate, HashMap<String, String> placeholders) {
        return socialPlatform.sendReply(this, messageTemplate, placeholders);
    }

    @Override
    public CompletableFuture<Boolean> sendReply(MessageKey message, String locale, HashMap<String, String> placeholders, ITransaction transaction) {
        return socialPlatform
            .getBridge()
            .getLocalizationService()
            .getMessage(locale, message, transaction)
            .thenCompose(messageTemplate -> sendReply(messageTemplate, placeholders));
    }

    @Override
    public ISocialPlatform getSocialPlatform() {
        return socialPlatform;
    }
}
