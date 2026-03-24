package io.github.kosyakmakc.socialBridgeVk;

import java.util.UUID;

import io.github.kosyakmakc.socialBridge.MinecraftPlatform.IModuleLoader;
import io.github.kosyakmakc.socialBridge.Modules.SocialModule;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeVk.MinecraftCommands.GetBotLinkCommand;
import io.github.kosyakmakc.socialBridgeVk.MinecraftCommands.SetTokenCommand;
import io.github.kosyakmakc.socialBridgeVk.MinecraftCommands.StatusCommand;
import io.github.kosyakmakc.socialBridgeVk.SocialCommands.HeartbeatCommand;
import io.github.kosyakmakc.socialBridgeVk.Translations.English;
import io.github.kosyakmakc.socialBridgeVk.Translations.Russian;

public class VkModule extends SocialModule {
    public static UUID MODULE_ID = UUID.fromString("76ec80c4-be05-4bf3-90b8-bdbd08dd963b");
    private static final Version compabilityVersion = new Version("0.10.1");
    private static final String ModuleName = "vk";

    public VkModule(IModuleLoader loader, Version version) {
        super(loader, compabilityVersion, version, MODULE_ID, ModuleName);

        addMinecraftCommand(new SetTokenCommand());
        addMinecraftCommand(new StatusCommand());
        addMinecraftCommand(new GetBotLinkCommand());

        addSocialCommand(new HeartbeatCommand());

        addTranslationSource(new English());
        addTranslationSource(new Russian());
    }
}
