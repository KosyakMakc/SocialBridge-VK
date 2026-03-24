package io.github.kosyakmakc.socialBridgeVk.Utils;

import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

// TODO - move to social-bridge
public class TranslationException extends Throwable {
    private MessageKey messageKey;

    public TranslationException(MessageKey messageKey) {
        super(messageKey.key());
        this.messageKey = messageKey;
    }

    public TranslationException(MessageKey messageKey, Throwable cause) {
        super(messageKey.key(), cause);
        this.messageKey = messageKey;
    }

    public TranslationException(MessageKey messageKey, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(messageKey.key(), cause, enableSuppression, writableStackTrace);
        this.messageKey = messageKey;
    }

    public MessageKey getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(MessageKey messageKey) {
        this.messageKey = messageKey;
    }
}
