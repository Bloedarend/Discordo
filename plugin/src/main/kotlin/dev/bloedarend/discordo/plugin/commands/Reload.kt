package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.handlers.CommandHandler
import dev.bloedarend.discordo.plugin.handlers.ConfigHandler
import dev.bloedarend.discordo.plugin.handlers.EventHandler
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Reload(private val plugin: Main) {

    @Command("dco", "discordo")
    @Subcommand("reload")
    @CommandPermission("discordo.command.reload")
    @Usage("dco reload")
    fun onCommand(sender: CommandSender) {
        val startTime = System.currentTimeMillis()

        // Unregister events and commands, because all config properties are initialised in the class.
        CommandHandler.unregisterCommands()
        EventHandler.unregisterListeners()

        // Reload configs.
        ConfigHandler.reloadConfigs()
        plugin.reload()

        MessageUtil.sendMessage("commands.reload.message", sender,
            Pair("%duration%", (System.currentTimeMillis() - startTime).toString())
        )
    }

}