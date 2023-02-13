package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Messages
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerDisconnect(configs: Configs, private val images: Images) : Listener {

    private val config = configs.getConfig("config")

    private val enabled = config?.getBoolean("minecraft.join.enabled") ?: false
    private val silentEnabled = config?.getBoolean("minecraft.join.silent") ?: false

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!enabled) return

        // Don't send a join message if the player has silent join enabled.
        if (silentEnabled && event.player.hasPermission("discordo.silent.leave")) return

        messages.sendMessage("minecraft.leave", event.player)
    }

    @EventHandler
    suspend fun onPlayerKick(event: PlayerKickEvent) {
        if (!enabled) return

        // Don't send a join message if the player has silent join enabled.
        if (silentEnabled && event.player.hasPermission("discordo.silent.leave")) return

        messages.sendMessage("minecraft.leave", event.player)
    }
}