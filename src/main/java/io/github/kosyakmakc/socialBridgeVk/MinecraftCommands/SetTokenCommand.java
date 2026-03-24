package io.github.kosyakmakc.socialBridgeVk.MinecraftCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandExecutionContext;
import io.github.kosyakmakc.socialBridgeVk.VkPlatform;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkPermissions;
import io.github.kosyakmakc.socialBridgeVk.Utils.TranslationException;

public class SetTokenCommand extends MinecraftCommandBase {

    public SetTokenCommand() {
        super("setupToken", VkMessageKey.SET_TOKEN_DESCRIPTION, VkPermissions.CAN_SET_LOGIN, List.of(CommandArgument.ofGreedyString("VK token (groupId:secret_token)")));
    }

    @Override
    public void execute(MinecraftCommandExecutionContext ctx, List<Object> parameters) {
        var sender = ctx.getSender();

        var token = (String) parameters.get(0);
        var placeholders = new HashMap<String, String>();
        if (validateToken(token)) {
            var setupTask = this.getBridge().getSocialPlatform(VkPlatform.class).setupToken(token, null);

            setupTask.thenCompose(isSuccess -> sender.sendMessage(VkMessageKey.SET_TOKEN_SUCCESS, sender.getLocale(), placeholders, null));

            setupTask
                .exceptionally(err -> {
                    if (err instanceof TranslationException translationException) {
                        sender.sendMessage(translationException.getMessageKey(), sender.getLocale(), placeholders, null);
                    } else {
                        sender.sendMessage(err.getMessage(), placeholders);
                    }

                    return true; // not used, just for close signature of lambda
                });
        }
        else {
            sender.sendMessage("please provide valid token (123456:secret_token)", placeholders);
        }
    }

    private boolean validateToken(String token) {
        var words = token.split(":");
        if (words.length != 2) {
            return false;
        }

        var rawGroupId = words[0];
        if (rawGroupId.length() == 0) {
            return false;
        }

        try {
            Long.parseLong(rawGroupId);
        }
        catch (NumberFormatException err) {
            return false;
        }

        var rawBotToken = words[1];
        if (!rawBotToken.matches("^[a-zA-Z0-9_.-]{220,220}$")) {
            return false;
        }

        return true;
    }
}
