package dev.bloedarend.discordo.kord

import dev.bloedarend.discordo.kord.events.MessageCreate
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Helpers
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.KordInitializationException
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.*
import net.md_5.bungee.api.ChatColor
import org.bukkit.plugin.Plugin

class Bot(private val plugin: Plugin, configs: Configs, messages: Messages, helpers: Helpers) {

    var client: Kord? = null
    private val token = configs.getConfig("token")?.getString("token")

    private val messageCreate = MessageCreate(plugin, configs, messages, helpers)

    suspend fun start() {
        initialize()

        if (client == null) return

        registerEvents()
        login()
    }

    private suspend fun initialize() {
        val errorMessage = listOf(
            "&8-------------------------------< &rDiscordo &8>-------------------------------",
            " &rBot token is invalid. If you need help with the plugin installation,",
            " follow the setup guide at &chttps://github.com/Bloedarend/Discordo",
            "&8--------------------------------------------------------------------------"
        ).joinToString("\n")

        try {
            client = Kord(token!!)
        } catch (exception: KordInitializationException) {
            plugin.server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n\n${errorMessage}\n"))
        }
    }

    private suspend fun registerEvents() {
        client!!.on<MessageCreateEvent> {
            messageCreate.onMessageCreate(this)
        }
    }

    private suspend fun login() {
        client!!.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

}