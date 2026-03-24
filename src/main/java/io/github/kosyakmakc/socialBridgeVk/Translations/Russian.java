package io.github.kosyakmakc.socialBridgeVk.Translations;

import java.util.List;

import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.LocalizationRecord;
import io.github.kosyakmakc.socialBridgeVk.Utils.VkMessageKey;

public class Russian implements ITranslationSource {
    @Override
    public String getLanguage() {
        return "ru";
    }

    @Override
    public List<LocalizationRecord> getRecords() {
        return List.of(
                new LocalizationRecord(VkMessageKey.SET_TOKEN_DESCRIPTION.key(), "Установить новый токен боту ВК, Если бот подключен, то он будет перезагружен с новым токеном."),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_SUCCESS.key(), "<green>Новый токен сохранен и применен.</green>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_CONFIG.key(), "<red>Не удалось сохранить токен в сервисе конфигураций.</red>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_STOP_BOT.key(), "<red>Не удалось остановить бота ВК, новый токен сохранен, но не был применен.</red>"),
                new LocalizationRecord(VkMessageKey.SET_TOKEN_FAILED_START_BOT.key(), "<red>Не удалось запустить бота ВК, новый токен сохранен, но не был применен.</red>"),

                new LocalizationRecord(VkMessageKey.BOT_STATUS_DESCRIPTION.key(), "Узнать статус подключения бота ВК."),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_CONNECTING.key(), "<yellow>Бот ВК подключается...</yellow>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_CONNECTED.key(), "<green>Бот ВК успешно подключен.</green>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_STOPPING.key(), "<yellow>Бот ВК останавливается...</yellow>"),
                new LocalizationRecord(VkMessageKey.BOT_STATUS_STOPPED.key(), "<red>Бот ВК остановлен.</red>")
        );
    }
}
