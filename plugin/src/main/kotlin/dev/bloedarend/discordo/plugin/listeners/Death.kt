package dev.bloedarend.discordo.plugin.listeners

import dev.bloedarend.discordo.plugin.Discordo
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.PlayerDeathEvent

class Death(private val discordo: Discordo) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val enityDamageEvent = event.entity.lastDamageCause

        if (enityDamageEvent == null) {
            discordo.sendImage(
                MessageUtil.getMessage("minecraft.death.unknown")
            )
        }

    }

}