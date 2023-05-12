package dev.bloedarend.discordo.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.scope
import dev.bloedarend.discordo.api.DiscordoAPI
import dev.bloedarend.discordo.api.DiscordoPlugin
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.*
import dev.kord.core.Kord
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin(), DiscordoPlugin {

    lateinit var discordo: Discordo
        private set
    lateinit var bot: Bot
        private set

    override fun onEnable() {
        ConfigUtil.loadConfigs(this)
        HelperUtil.testFonts(this)

        discordo = Discordo(this)
        discordo.scope = this.scope

        bot = Bot(this)

        CommandUtil.createCommandHandler(this)
        CommandUtil.registerCommands(this)
        EventUtil.registerListeners(this)

        startBot()

        if (HelperUtil.getVersion(this) < 16) {
            val errorMessage = listOf(
                "&8-------------------------------< &rDiscordo &8>-------------------------------",
                " &rVersion not supported. This plugin may not work as expected on servers",
                " running versions below &c1.16&r. If the plugin does not work for your version,",
                " can fork this plugin at &chttps://github.com/Bloedarend/Discordo",
                "&8--------------------------------------------------------------------------"
            ).joinToString("\n")

            server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n\n${errorMessage}\n"))
        }
    }

    private fun startBot(oldClient: Kord? = null) {
        launch {
            oldClient?.logout()
            bot.start()
        }
    }

    fun reload() {
        // Reload all utils.
        MessageUtil.reload()
        discordo.reload()

        // Restart the bot.
        startBot(bot.client)

        // Register commands and events.
        CommandUtil.registerCommands(this)
        EventUtil.registerListeners(this)
    }

    override fun getAPI(): DiscordoAPI {
        if (::discordo.isInitialized) {
            return discordo
        } else {
            throw Exception("DiscordoAPI has not been initialized yet!")
        }
    }

}