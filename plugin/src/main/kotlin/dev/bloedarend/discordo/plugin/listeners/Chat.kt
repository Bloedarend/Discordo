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
import java.io.InputStream

class PlayerAsyncChat(configs: Configs, private val bot: Bot, private val images: Images) : Listener {

    private val config: YamlDocument? = configs.getConfig("config")

    private val enabled = config?.getBoolean("minecraft.enabled") ?: false
    private val translateColorCodes = config?.getBoolean("minecraft.translate-color-codes") ?: false

    @EventHandler
    suspend fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!enabled) return
        if (bot.client == null) return

        val player = event.player
        val message =
            if (translateColorCodes) {
                event.message
            } else {
                // Remove the color codes from the message.
                event.message.replace("${ChatColor.COLOR_CHAR}","&").replace(Regex("&([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6})"), "")
            }

        val text = String.format(event.format, player.displayName.replace("${ChatColor.COLOR_CHAR}","&"), message)

        bot.client!!.guilds.toList()[0].systemChannel?.createMessage {
            val inputStream: InputStream = images.getInputStream(text)

            addFile("discordo.png", ChannelProvider {
                inputStream.toByteReadChannel()
            })
        }
    }

}