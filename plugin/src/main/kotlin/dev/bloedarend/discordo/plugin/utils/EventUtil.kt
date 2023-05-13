package dev.bloedarend.discordo.plugin.utils

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.listeners.Chat
import dev.bloedarend.discordo.plugin.listeners.Death
import dev.bloedarend.discordo.plugin.listeners.JoinLeave
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

class EventUtil private constructor() {

    companion object {
        fun registerListeners(plugin: Main) {
            Bukkit.getPluginManager().registerSuspendingEvents(Chat(plugin), plugin)
            Bukkit.getPluginManager().registerSuspendingEvents(Death(plugin), plugin)
            Bukkit.getPluginManager().registerSuspendingEvents(JoinLeave(plugin), plugin)

            // Disable the PlayerMoveEvent listener when it is not needed.
            unregisterPlayerMove()
        }

        fun unregisterListeners() {
            HandlerList.unregisterAll()
        }

        private fun unregisterPlayerMove() { }
    }

}