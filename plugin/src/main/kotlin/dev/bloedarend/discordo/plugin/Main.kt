package dev.bloedarend.discordo.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.InputStream

class Main: JavaPlugin() {

    private val helpers = Helpers()
    private val configs = Configs(this)

    private lateinit var messages: Messages
    private lateinit var images: Images
    private lateinit var events: Events
    private lateinit var commands: Commands
    private lateinit var bot: Bot

    override fun onEnable() {
        if (helpers.getVersion(this) >= 16) {
            configs.loadConfigs(this)

            messages = Messages(configs)
            images = Images(this, configs, helpers)
            bot = Bot(this, configs, messages, helpers, images)
            events = Events(configs, messages, bot, images)
            commands = Commands(configs, events, messages, this, bot)

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

    private fun startBot(oldClient: Kord? = null) {
        launch {
            oldClient?.logout()
            bot.start()
        }
    }

    fun reload() {
        // Store the original client.
        val client = bot.client

        // Pass the new instance of the configs to the utils.
        messages = Messages(configs)
        images = Images(this, configs, helpers)
        bot = Bot(this as Plugin, configs, messages, helpers, images)
        events = Events(configs, messages, bot, images)
        commands = Commands(configs, events, messages, this, bot)

        // Start the bot.
        startBot(client)

        // Register commands and events.
        commands.registerCommands()
        events.registerListeners(this)
    }

}