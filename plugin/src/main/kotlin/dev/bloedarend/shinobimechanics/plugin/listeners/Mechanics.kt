package dev.bloedarend.shinobimechanics.plugin.listeners

import dev.bloedarend.shinobimechanics.plugin.utils.Configs
import dev.bloedarend.shinobimechanics.plugin.utils.Modules
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.entity.Fish
import org.bukkit.entity.FishHook
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent

class Mechanics(configs: Configs, private val modules: Modules) : Listener {

    val config: YamlDocument? = configs.getConfig("config")

    @EventHandler
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        val player = event.player

        player.sendMessage("sneakEvent")
        if (player.isSneaking) {
//            player.sendMessage("isSneaking")
//            val fishHook: FishHook = player.launchProjectile(Fish::class.java)
//            fishHook.velocity = player.location.direction.multiply(2)
            modules.mechanics.spawnWireKunai(player)
        }
    }

}