package dev.bloedarend.shinobimechanics.plugin.utils

import dev.bloedarend.shinobimechanics.plugin.listeners.Mechanics
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

class Events(private val configs: Configs, private val helpers: Helpers, private val messages: Messages, private val modules: Modules) {

    fun registerListeners(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(Mechanics(configs, modules), plugin)

        // Disable the PlayerMoveEvent listener when it is not needed.
        unregisterPlayerMove()
    }

    fun unregisterListeners() {
        HandlerList.unregisterAll()
    }

    private fun unregisterPlayerMove() { }

}