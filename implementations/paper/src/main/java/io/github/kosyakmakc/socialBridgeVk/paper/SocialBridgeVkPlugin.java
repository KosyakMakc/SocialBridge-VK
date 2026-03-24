package io.github.kosyakmakc.socialBridgeVk.paper;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.kosyakmakc.socialBridge.SocialBridge;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.IModuleLoader;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeVk.VkModule;
import io.github.kosyakmakc.socialBridgeVk.VkPlatform;

public class SocialBridgeVkPlugin extends JavaPlugin implements IModuleLoader {
    private final VkPlatform platform;
    private final VkModule module;
    
    public SocialBridgeVkPlugin() {
        platform = new VkPlatform();
        module = new VkModule(this, new Version(this.getPluginMeta().getVersion()));

        SocialBridge.INSTANCE.connectModule(module).join();
        SocialBridge.INSTANCE.connectSocialPlatform(platform).join();
    }

    @Override
    public void onDisable() {
        SocialBridge.INSTANCE.disconnectSocialPlatform(platform).join();
        SocialBridge.INSTANCE.disconnectModule(module).join();
    }
}
