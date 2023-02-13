package dev.bloedarend.discordo.plugin.utils

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.listeners.Chat
import dev.bloedarend.discordo.plugin.listeners.JoinLeave
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

class Events(private val configs: Configs, private val messages: Messages, private val bot: Bot, private val images: Images) {

    fun registerListeners(plugin: Plugin) {
        Bukkit.getPluginManager().registerSuspendingEvents(Chat(configs, bot, images), plugin)
        Bukkit.getPluginManager().registerSuspendingEvents(JoinLeave(configs, messages, bot, images), plugin)

        // Disable the PlayerMoveEvent listener when it is not needed.
        unregisterPlayerMove()
    }

    fun unregisterListeners() {
        HandlerList.unregisterAll()
    }

    private fun unregisterPlayerMove() { }

}