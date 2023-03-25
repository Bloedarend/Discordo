package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.Messages
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Invite(private val messages: Messages, private val bot: Bot) {

    @Command("dco", "discordo")
    @Subcommand("invite")
    @CommandPermission("discordo.command.invite")
    @Usage("dco invite")
    fun onCommand(sender: CommandSender) {
        messages.sendMessage("commands.invite.message", sender,
            Pair("%invite%", "https://discord.com/oauth2/authorize?client_id=${bot.client!!.selfId}&permissions=532643441745&scope=bot%20applications.commands")
        )
    }

}