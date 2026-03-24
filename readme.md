# SocialBridge-VK
## It is social platform connector to [VK](https://vk.com/) for [SocialBridge](https://github.com/KosyakMakc/social-bridge) minecraft plugin

### this connector provide commands for setup bot and check his health

### Commands for minecraft:

| Command literal| Permission node            | Description                                                                   |
|----------------|----------------------------|-------------------------------------------------------------------------------|
| /vk setToken   | SocialBridge.VK.setToken   | Save new token to SocialBridge configuration and reconnect bot with new token |
| /vk status     | SocialBridge.VK.status     | Provide information about current connection bot to VK                        |
| /vk getBotLink | SocialBridge.VK.getBotLink | Get direct link to this bot(public group) if token exist and bot connected    |

### Commands for VK:

| Command literal      | Description                                                                   |
|----------------------|-------------------------------------------------------------------------------|
| /vk_heartbeat  | Check activity bot in VK |

## API for developers

### You can connect API of this module for your purposes
```
repositories {
    maven {
        name = "gitea"
        url = "https://git.kosyakmakc.ru/api/packages/kosyakmakc/maven"
    }
}
dependencies {
    compileOnly "io.github.kosyakmakc:SocialBridge-VK:0.10.+"
}
```

### via `ISocialBridge.getSocialPlatform(VKModule.class)` you can access built-in module and use API (Recommended)
### via `ISocialBridge.getSocialPlatform(VKPlatform.class)` you can access this connector and use VK-specific API
