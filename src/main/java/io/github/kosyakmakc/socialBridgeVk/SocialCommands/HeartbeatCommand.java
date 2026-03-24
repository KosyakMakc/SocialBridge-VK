package io.github.kosyakmakc.socialBridgeVk.SocialCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandBase;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandExecutionContext;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

public class HeartbeatCommand extends SocialCommandBase {

    public HeartbeatCommand() {
        super("heartbeat", MessageKey.EMPTY);
    }

    @Override
    public void execute(SocialCommandExecutionContext ctx, List<Object> args) {
        ctx.getSocialMessage().sendReply("ответ на...", new HashMap<>());
        var channelId = ctx.getSocialMessage().getChannelId();
        ctx.getSocialPlatform().sendMessage(channelId, "напрямую", new HashMap<>());
    }

}
