package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Broadcast(private val plugin: Main) {

    @Command("dco", "discordo")
    @Subcommand("broadcast", "bc")
    @CommandPermission("discordo.command.broadcast")
    @Usage("dco broadcast")
    fun onCommand(sender: CommandSender, message: String) {
        plugin.discordo.sendImage(
            MessageUtil.getMessage("commands.broadcast.format", sender, Pair("%message%", message))
        )

        MessageUtil.sendMessage("commands.broadcast.message", sender)
    }

}