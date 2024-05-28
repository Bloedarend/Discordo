package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage
import revxrsal.commands.bukkit.annotation.CommandPermission

class Broadcast(private val plugin: Main) {

    private val config: YamlDocument? = ConfigUtil.getConfig("config")

    @Command("dco", "discordo")
    @Subcommand("broadcast", "bc")
    @CommandPermission("discordo.command.broadcast")
    @Usage("dco broadcast")
    fun onCommand(sender: CommandSender, message: String) {
        if (config?.getBoolean("minecraft.image.enabled") == true) {
            plugin.discordo.sendImage(
                MessageUtil.getMessage("commands.broadcast.format", sender, Pair("%message%", message))
            )
        } else {
            plugin.discordo.sendText(ChatColor.stripColor("**[BROADCAST]** $message") ?: return)
        }

        MessageUtil.sendMessage("commands.broadcast.message", sender)
    }

}