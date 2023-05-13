package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class Death(private val plugin: Main) : Listener {

    private val config = ConfigUtil.getConfig("config")

    private val enabled = config?.getBoolean("minecraft.enabled") ?: true
    private val deathEnabled = config?.getBoolean("minecraft.death.enabled") ?: false

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!enabled || !deathEnabled) return

        val message = event.deathMessage ?: return Bukkit.getLogger().warning("The death message is null, so it cannot be sent to Discord.\"")

        plugin.discordo.sendImage(message)
    }

}