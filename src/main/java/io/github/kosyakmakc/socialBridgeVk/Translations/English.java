package io.github.kosyakmakc.socialBridgeVk.Translations;

import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.LocalizationRecord;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;

import java.util.List;

public class English implements ITranslationSource {
    @Override
    public String getLanguage() {
        return LocalizationService.defaultLocale;
    }

    @Override
    public List<LocalizationRecord> getRecords() {
        return List.of(
                new LocalizationRecord(VkMessageKey.SET_TOKEN_DESCRIPTION.key(), "Setup new token of VK bot, existed bot also will be reloaded with new token"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_SUCCESS.key(), "<green>New token saved and applied.</green>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_CONFIG.key(), "<red>Failed to save token to configuration service.</red>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_STOP_BOT.key(), "<red>Failed to stop VK bot, new token saved, but not applied.</red>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_START_BOT.key(), "<red>Failed to start VK bot, new token saved, but not applied.</red>"),

                new LocalizationRecord(VkMessageKey.BOT_STATUS_DESCRIPTION.key(), "View status of VK bot connection"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_CONNECTING.key(), "<yellow>VK bot are connecting...</yellow>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_CONNECTED.key(), "<green>VK bot successfully connected.</green>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_STOPPING.key(), "<yellow>VK bot are stopping...</yellow>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_STOPPED.key(), "<red>VK bot stopped.</red>")
        );
    }
}
