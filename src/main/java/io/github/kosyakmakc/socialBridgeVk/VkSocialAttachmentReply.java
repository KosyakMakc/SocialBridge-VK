package io.github.kosyakmakc.socialBridgeVk;

import com.vk.api.sdk.objects.messages.ForeignMessage;

import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialAttachmentReply;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialMessage;

public class VkSocialAttachmentReply implements ISocialAttachmentReply {
    private final ForeignMessage replyMessage;
    private final VkPlatform socialPlatform;

    public VkSocialAttachmentReply(VkPlatform socialPlatform, ForeignMessage replyMessage) {
        this.socialPlatform = socialPlatform;
        this.replyMessage = replyMessage;
    }

    @Override
    public ISocialMessage getReply() {
        return new VkSocialMessage(socialPlatform, replyMessage);
    }

}
