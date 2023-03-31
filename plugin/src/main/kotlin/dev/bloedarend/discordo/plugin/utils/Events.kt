package dev.bloedarend.discordo.plugin.utils

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.listeners.Chat
import dev.bloedarend.discordo.plugin.listeners.JoinLeave
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

class Events(plugin: Main) {

    private val configs = plugin.configs
    private val bot = plugin.bot
    private val messages = plugin.messages
    private val discordo = plugin.discordo

    fun registerListeners(plugin: Plugin) {
        Bukkit.getPluginManager().registerSuspendingEvents(Chat(configs, bot, discordo), plugin)
        Bukkit.getPluginManager().registerSuspendingEvents(JoinLeave(configs, messages, bot, discordo), plugin)

        // Disable the PlayerMoveEvent listener when it is not needed.
        unregisterPlayerMove()
    }

    fun unregisterListeners() {
        HandlerList.unregisterAll()
    }

    private fun unregisterPlayerMove() { }

}