package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.handlers.ConfigHandler
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage

class Help {

    private val config: YamlDocument? = ConfigHandler.getConfig("language")

    private val useSeparator = config?.getBoolean("command.help.use-separator") ?: true

    @Command("dco", "discordo")
    @Subcommand("help")
    @Usage("dco help")
    fun onCommand(sender: CommandSender) {
        val commands: List<Pair<String, String>> = listOf(
            Pair("dco broadcast <message>", MessageUtil.getMessage("commands.broadcast.description", sender)),
            Pair("dco help", MessageUtil.getMessage("commands.help.description", sender)),
            Pair("dco invite", MessageUtil.getMessage("commands.invite.description", sender)),
            Pair("dco reload", MessageUtil.getMessage("commands.reload.description", sender))
        )

        if (useSeparator) MessageUtil.sendMessage("commands.help.separator", sender)

        MessageUtil.sendMessage("commands.help.title", sender)

        for (command in commands) {
            MessageUtil.sendMessage("commands.help.line", sender,
                Pair("%command%", command.first),
                Pair("%command_description%", command.second)
            )
        }

        if (useSeparator) MessageUtil.sendMessage("commands.help.separator", sender)
    }

}