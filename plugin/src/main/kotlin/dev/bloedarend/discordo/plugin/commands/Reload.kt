package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.utils.Commands
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Events
import dev.bloedarend.discordo.plugin.utils.Messages
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Reload(private val commands: Commands, private val configs: Configs, private val events: Events, private val messages: Messages, private val plugin: Plugin) {

    @Command("dco", "discordo")
    @Subcommand("reload")
    @CommandPermission("discordo.command.reload")
    @Usage("dco reload")
    fun onCommand(sender: CommandSender) {
        val startTime = System.currentTimeMillis()

        // Unregister events and commands, because all config properties are initialised in the class.
        commands.unregisterCommands()
        events.unregisterListeners()

        // Reload configs.
        configs.reloadConfigs()

        messages.sendMessage("commands.reload.success", sender,
            Pair("%duration%", (System.currentTimeMillis() - startTime).toString())
        )
    }

}