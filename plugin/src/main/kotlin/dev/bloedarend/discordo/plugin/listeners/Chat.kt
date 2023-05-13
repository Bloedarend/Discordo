package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class Chat(private val plugin: Main) : Listener {

    private val config: YamlDocument? = ConfigUtil.getConfig("config")

    private val enabled = config?.getBoolean("minecraft.enabled") ?: true
    private val translateColorCodes = config?.getBoolean("minecraft.translate-color-codes") ?: false

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!enabled) return
        if (plugin.bot.client == null) return // Can't send messages if the bot client is not ready.

        val player = event.player
        var message = event.message.replace("${ChatColor.COLOR_CHAR}","&") // Turn chat colors back to color codes.

        if (!translateColorCodes) {
            // Remove the color codes from the message.
            message = message.replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), "")
        }

        val text = String.format(event.format, player.displayName.replace("${ChatColor.COLOR_CHAR}","&"), message)

        plugin.discordo.sendImage(text)
    }

}