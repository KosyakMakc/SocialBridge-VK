package io.github.kosyakmakc.socialBridgeVk.MinecraftCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandExecutionContext;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeVk.VkPlatform;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkPermissions;

public class StatusCommand extends MinecraftCommandBase {

    public StatusCommand() {
        super("status", VkMessageKey.BOT_STATUS_DESCRIPTION, VkPermissions.CAN_STATUS);
    }

    @Override
    public void execute(MinecraftCommandExecutionContext ctx, List<Object> args) {
        var sender = ctx.getSender();
        var platform = getBridge().getSocialPlatform(VkPlatform.class);
        MessageKey messageKey;
        switch (platform.getBotState()) {
            case Started:
                messageKey = VkMessageKey.BOT_STATUS_CONNECTED;
                break;
            case Starting:
                messageKey = VkMessageKey.BOT_STATUS_CONNECTING;
                break;
            case Stopped:
                messageKey = VkMessageKey.BOT_STATUS_STOPPED;
                break;
            case Stopping:
                messageKey = VkMessageKey.BOT_STATUS_STOPPING;
                break;
            default:
                throw new RuntimeException("Unexpected VK bot state");

        }
        sender.sendMessage(messageKey, sender.getLocale(), new HashMap<String, String>(), null);
    }

}
