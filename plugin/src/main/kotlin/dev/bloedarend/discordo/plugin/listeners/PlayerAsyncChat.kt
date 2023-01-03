package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Images
import dev.dejvokep.boostedyaml.YamlDocument
import dev.kord.core.behavior.channel.createMessage
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.flow.toList
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.Plugin
import java.io.InputStream

class PlayerAsyncChat(private val configs: Configs, private val plugin: Plugin, private val bot: Bot, private val images: Images) : Listener {

    private val config: YamlDocument? = configs.getConfig("config")

    @EventHandler
    suspend fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (bot.client == null) return

        val player = event.player
        val message = String.format(event.format, player.displayName.replace("${ChatColor.COLOR_CHAR}","&"), event.message)

        bot.client!!.guilds.toList()[0].systemChannel?.createMessage {
            val inputStream: InputStream = images.getInputStream(message)

            addFile("discordo.png", ChannelProvider {
                inputStream.toByteReadChannel()
            })
        }
    }

}