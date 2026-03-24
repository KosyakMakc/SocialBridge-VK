package io.github.kosyakmakc.socialBridgeVk.MinecraftCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandExecutionContext;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeVk.BotState;
import io.github.kosyakmakc.socialBridgeVk.VkPlatform;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkPermissions;

public class GetBotLinkCommand extends MinecraftCommandBase {

    public GetBotLinkCommand() {
        super("getBotLink", MessageKey.EMPTY, VkPermissions.CAN_GET_BOT_LINK);
    }

    @Override
    public void execute(MinecraftCommandExecutionContext ctx, List<Object> args) {
        var bridge = getBridge();
        var platform = bridge.getSocialPlatform(VkPlatform.class);
        if (platform.getBotState() == BotState.Started) {
            var botId = platform.getGroupId();

            var url = "https://vk.com/im/convo/" + botId; // link to community chat
            var text = "@" + platform.getGroupName();

            var template = "<#7878ff><underlined><hover:show_text:'<green>" + url + "'><click:open_url:'" + url + "'>" + text;
            ctx.getSender().sendMessage(template, new HashMap<>());
        }
        else {
            var locale = ctx.getSender().getLocale();
            ctx.getSender().sendMessage(VkMessageKey.BOT_STATUS_STOPPED, locale, new HashMap<>(), null);
        }
    }

}
