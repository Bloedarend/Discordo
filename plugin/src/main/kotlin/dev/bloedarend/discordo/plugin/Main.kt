package dev.bloedarend.discordo.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.scope
import dev.bloedarend.discordo.api.Discordo
import dev.bloedarend.discordo.api.IDiscordo
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.*
import dev.kord.core.Kord
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {

    lateinit var helpers: Helpers
        private set
    lateinit var configs: Configs
        private set
    lateinit var messages: Messages
        private set
    lateinit var discordo: Discordo
        private set
    lateinit var bot: Bot
        private set
    lateinit var events: Events
        private set
    lateinit var commands: Commands
        private set

    override fun onEnable() {
        helpers = Helpers()
        configs = Configs(this)

        // Load the configs before initialising the other utils.
        configs.loadConfigs(this)

        // The order of these is important.
        messages = Messages(this)
        discordo = Discordo(this)
        bot = Bot(this)
        events = Events(this)
        commands = Commands(this)

        discordo.scope = this.scope

        commands.registerCommands()
        events.registerListeners(this)

        startBot()

        // Register API to Bukkit services.
        Bukkit.getServer().servicesManager.register(IDiscordo::class.java, discordo, this, ServicePriority.High)

        if (helpers.getVersion(this) < 16) {
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
        messages.reload()
        discordo.reload()

        // Restart the bot.
        startBot(bot.client)

        // Register commands and events.
        commands.registerCommands()
        events.registerListeners(this)
    }

}