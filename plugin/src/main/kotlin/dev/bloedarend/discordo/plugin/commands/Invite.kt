package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Invite(private val plugin: Main) {

    @Command("dco", "discordo")
    @Subcommand("invite")
    @CommandPermission("discordo.command.invite")
    @Usage("dco invite")
    fun onCommand(sender: CommandSender) {
        MessageUtil.sendMessage("commands.invite.message", sender,
            Pair("%invite%", "https://discord.com/oauth2/authorize?client_id=${plugin.bot.client!!.selfId}&permissions=532643441745&scope=bot%20applications.commands")
        )
    }

}