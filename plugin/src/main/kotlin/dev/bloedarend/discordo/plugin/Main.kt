package dev.bloedarend.discordo.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.*
import dev.kord.core.Kord
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {

    val helpers = Helpers()
    private val configs = Configs(this)

    lateinit var messages: Messages
    lateinit var images: Images
    lateinit var events: Events
    lateinit var commands: Commands
    lateinit var bot: Bot

    override fun onEnable() {
        if (helpers.getVersion(this) >= 16) {
            configs.loadConfigs(this)

            messages = Messages(configs)
            images = Images(this, configs, helpers)
            bot = Bot(this, configs, messages, helpers)
            events = Events(configs, messages, bot, images)
            commands = Commands(configs, events, messages, this)

            startBot()

            commands.registerCommands()
            events.registerListeners(this)
        }

        else {
            val errorMessage = listOf(
                "&8-------------------------------< &rDiscordo &8>-------------------------------",
                " &rVersion not supported. This plugin will only work for servers running",
                " version &c1.16&r or higher. If you wish to support your version, you",
                " can fork this plugin at &chttps://github.com/Bloedarend/Discordo",
                "&8--------------------------------------------------------------------------"
            ).joinToString("\n")

            server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n\n${errorMessage}\n"))
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    fun startBot(oldClient: Kord? = null) {
        launch {
            oldClient?.logout()
            bot.start()
        }
    }
}