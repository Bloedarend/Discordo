package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.api.Discordo
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Messages
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class JoinLeave(configs: Configs, private val messages: Messages, private val bot: Bot, private val discordo: Discordo) : Listener {

    private val config = configs.getConfig("config")

    private val joinEnabled = config?.getBoolean("minecraft.join.enabled") ?: false
    private val silentJoinEnabled = config?.getBoolean("minecraft.join.silent") ?: false
    private val leaveEnabled = config?.getBoolean("minecraft.join.enabled") ?: false
    private val silentLeaveEnabled = config?.getBoolean("minecraft.join.silent") ?: false

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!joinEnabled) return
        val player = event.player

        // Don't send a join message if the player has silent join enabled.
        if (silentJoinEnabled && player.hasPermission("discordo.silent.join")) return

        sendImage("minecraft.join", player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!leaveEnabled) return
        val player = event.player

        // Don't send a leave message if the player has silent leave enabled.
        if (silentLeaveEnabled && player.hasPermission("discordo.silent.leave")) return

        sendImage("minecraft.leave", player)
    }

    private fun sendImage(path: String, player: Player) {
        val message = messages.getMessage(path, player).replace("${ChatColor.COLOR_CHAR}","&")

        discordo.sendImage(message)
    }
}