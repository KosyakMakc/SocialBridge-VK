package io.github.kosyakmakc.socialBridgeVk;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Modules.ISocialModule;
import java.util.HashMap;
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

        // Commands handling
        if (tryCommandHandle(update)) {
            return;
        }
        
        // TODO Messages handling in future
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
