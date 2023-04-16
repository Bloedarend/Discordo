# Discordo
Discordo is a plugin that allows users to communicate with each other on both Discord and Minecraft. What makes it special is the implementation of image manipulation, which displays messages to Discord as an image using the Minecraft chat format.

## Installation
Follow these steps to install the plugin to your Minecraft and Discord server. For support, please join the [Discord](https://bloedarend.dev/discord) server.
1. Compile the plugin or download the latest jar at the [Spigot](https://www.spigotmc.org/resources/discordo.108824/) page.
2. Upload the jar file to your `plugins` folder and restart the server. When the server is restarting, you should see a message from Discordo stating that the bot token is invalid.
3. Go to the [Discord Developers](https://discord.com/developers/applications) page and create a new application. Give it any name you want and agree to the Discord Developer TOS and Policy.
4. Navigate to the 'Bot' tab on the left and add a bot to your application.
5. Click on 'copy' to copy the bot token to your clipboard. If there is no visible 'copy' button, you need to reset your token first.
6. For security reasons, disable the 'PUBLIC BOT' option, which prevents other users from using your client id to invite the bot to their own servers.
7. Scroll down to the 'Privileged Gateway Intents' and enable all three intents.
8. Go back to your `plugins` folder and look for a directory named `Discordo`. Go into this directory and open the `token.yml` file.
9. Paste the bot token from your clipboard into the `token.yml` file next to `token: ` and reload the plugin with the '/dco reload' command.
10. Run the `/dco invite` command and click on the url, then choose a server to invite the bot to.
11. Right click on your server and click on 'Copy Server ID'. If you do not see a button to copy the server id, you may need to enable Discord Developer Mode. To do this, go to settings and then to the 'Advanced' tab. Here you can enable the Developer Mode.
12. Open the `config.yml` file and paste your server id next to `guild-id: `.
13. Create a channel or choose an existing one and copy the channel id. Paste the id into the same `config.yml` file next to `channel-id: `.
14. Make sure the bot has permission to 'View Channel', 'Send Messages' and 'Attach Files'.
15. Reload the plugin with the '/dco reload' command. Players should not be able to communicate between your Minecraft server and the Discord channel.

##API
The Discordo API offers the ability for other plugin developers to send their own messages to the Discord server as an image using the Minecraft chat format.
### Adding Discordo to your plugin:
1. Add Discordo as a dependency in your `plugin.yml` file.
```yml
depend: # Your plugin will not enable if the server does not have the Discordo plugin.
  - Discord
```
```yml
softdepend: # Your plugin will enable without Discordo.
  - Discordo
```
2. Add the Discordo api as a dependency in your `pom.xml` file.
```xml
<!-- Discord api -->
<dependency>
    <groupId>dev.bloedarend</groupId>
    <artifactId>discordo-api</artifactId>
    <version>1.1.0</version>
</dependency>
```
### Using Discordo in your plugin.
Using Discordo simple. First you want to check if the Discordo plugin is enabled. If this is the case, cast the plugin to `DiscordoPlugin` and get the api with the `DiscordoPlugin#getAPI()` method.

```java
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import dev.bloedarend.discordo.api.DiscordoAPI;
import dev.bloedarend.discordo.api.DiscordoPlugin;

public final class MyPlugin extends JavaPlugin {
    
    public DiscordoAPI discordoAPI;

    @Override
    public void onEnable() {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Discordo")) {
            DiscordoPlugin discordoPlugin = (DiscordoPlugin) Bukkit.getServer().getPluginManager().getPlugin("Discordo");
            assert discordoPlugin != null;
            
            discordoAPI = discordoPlugin.getAPI();
        }
        
        if (discordoAPI != null) {
            discordoAPI.sendImage("&aDiscordo is successfully working!");
        }
    }
}
```
