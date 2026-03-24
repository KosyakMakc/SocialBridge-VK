package io.github.kosyakmakc.socialBridgeVk.Utils;

import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeVk.VkModule;

public class VkMessageKey {
    public static final MessageKey SET_TOKEN_DESCRIPTION = new MessageKey(VkModule.MODULE_ID, "set_token_description");
    public static final MessageKey SET_TOKEN_SUCCESS = new MessageKey(VkModule.MODULE_ID, "set_token_success");
    public static final MessageKey SET_TOKEN_FAILED_CONFIG = new MessageKey(VkModule.MODULE_ID, "set_token_failed_config");
    public static final MessageKey SET_TOKEN_FAILED_STOP_BOT = new MessageKey(VkModule.MODULE_ID, "set_token_failed_stop_bot");
    public static final MessageKey SET_TOKEN_FAILED_START_BOT = new MessageKey(VkModule.MODULE_ID, "set_token_failed_start_bot");

    public static final MessageKey BOT_STATUS_DESCRIPTION = new MessageKey(VkModule.MODULE_ID, "bot_status_description");
    public static final MessageKey BOT_STATUS_CONNECTING = new MessageKey(VkModule.MODULE_ID, "bot_status_connecting");
    public static final MessageKey BOT_STATUS_CONNECTED = new MessageKey(VkModule.MODULE_ID, "bot_status_connected");
    public static final MessageKey BOT_STATUS_STOPPING = new MessageKey(VkModule.MODULE_ID, "bot_status_stopping");
    public static final MessageKey BOT_STATUS_STOPPED = new MessageKey(VkModule.MODULE_ID, "bot_status_stopped");

    // public static final MessageKey BOT_STARTED = new MessageKey(VkModule.MODULE_ID, "bot_started");
    // public static final MessageKey BOT_STOPPED = new MessageKey(VkModule.MODULE_ID, "bot_stopped");
}
