package dev.bloedarend.shinobimechanics.plugin.commands

import dev.bloedarend.shinobimechanics.plugin.utils.Commands
import dev.bloedarend.shinobimechanics.plugin.utils.Configs
import dev.bloedarend.shinobimechanics.plugin.utils.Events
import dev.bloedarend.shinobimechanics.plugin.utils.Messages
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Reload(private val commands: Commands, private val configs: Configs, private val events: Events, private val messages: Messages, private val plugin: Plugin) {

    @Command("sm", "shinobim", "shinobimechanics")
    @Subcommand("reload")
    @CommandPermission("shinobimechanics.admin")
    @Usage("sm reload")
    fun onCommand(player: Player) {
        val startTime = System.currentTimeMillis()

        // Unregister events and commands, because all config properties are initialised in the class.
        commands.unregisterCommands()
        events.unregisterListeners()

        // Reload configs and register all events and commands.
        configs.reloadConfigs()
        commands.registerCommands()
        events.registerListeners(plugin)

        messages.sendMessage("commands.reload.success", player,
            Pair("%duration%", (System.currentTimeMillis() - startTime).toString())
        )
    }

}