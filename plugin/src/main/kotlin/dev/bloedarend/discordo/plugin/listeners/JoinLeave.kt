package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class JoinLeave(private val plugin: Main) : Listener {

    private val config = ConfigUtil.getConfig("config")

    private val enabled = config?.getBoolean("minecraft.enabled") ?: true
    private val joinEnabled = config?.getBoolean("minecraft.join.enabled") ?: true
    private val joinUseCustom = config?.getBoolean("minecraft.join.use-custom") ?: true
    private val silentJoinEnabled = config?.getBoolean("minecraft.join.silent") ?: false
    private val leaveEnabled = config?.getBoolean("minecraft.leave.enabled") ?: true
    private val leaveUseCustom = config?.getBoolean("minecraft.leave.use-custom") ?: true
    private val silentLeaveEnabled = config?.getBoolean("minecraft.leave.silent") ?: false

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!enabled || !joinEnabled) return
        val player = event.player

        // Don't send a join message if the player has silent join enabled.
        if (silentJoinEnabled && player.hasPermission("discordo.silent.join")) return

        val message =
            if (joinUseCustom) MessageUtil.getMessage("minecraft.join", player).replace("${ChatColor.COLOR_CHAR}","&")
            else event.joinMessage ?: return Bukkit.getLogger().warning("The join message is null, so it cannot be sent to Discord.")

        plugin.discordo.sendImage(message)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!enabled || !leaveEnabled) return
        val player = event.player

        // Don't send a leave message if the player has silent leave enabled.
        if (silentLeaveEnabled && player.hasPermission("discordo.silent.leave")) return

        val message =
            if (leaveUseCustom) MessageUtil.getMessage("minecraft.leave", player).replace("${ChatColor.COLOR_CHAR}","&")
            else event.quitMessage ?: return Bukkit.getLogger().warning("The leave message is null, so it cannot be sent to Discord.")

        plugin.discordo.sendImage(message)
    }
}