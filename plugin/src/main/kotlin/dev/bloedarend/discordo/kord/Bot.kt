package dev.bloedarend.discordo.kord

import dev.bloedarend.discordo.kord.events.MessageCreate
import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.KordInitializationException
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import net.md_5.bungee.api.ChatColor

class Bot(private val plugin: Main) {

    var client: Kord? = null
        private set
    private lateinit var token: String

    suspend fun start() {
        initialize()

        if (client == null) return

        registerEvents()
        login()
    }

    private suspend fun initialize() {
        token = ConfigUtil.getConfig("token")?.getString("token") ?: ""

        val errorMessage = listOf(
            "&8-------------------------------< &rDiscordo &8>-------------------------------",
            " &rBot token is invalid. If you need help with the plugin installation,",
            " follow the setup guide at &chttps://github.com/Bloedarend/Discordo",
            "&8--------------------------------------------------------------------------"
        ).joinToString("\n")

        try {
            client = Kord(token)
        } catch (exception: KordInitializationException) {
            plugin.server.consoleSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n\n${errorMessage}\n"))
            exception.printStackTrace()
        }
    }

    private suspend fun registerEvents() {
        client!!.on<MessageCreateEvent> {
            MessageCreate(plugin).onMessageCreate(this)
        }
    }

    @OptIn(PrivilegedIntent::class)
    private suspend fun login() {
        client!!.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
            intents += Intent.GuildVoiceStates
            intents += Intent.GuildMembers
        }
    }

}