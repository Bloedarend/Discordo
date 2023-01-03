package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.command.CommandSender
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage

class Help(configs: Configs, private val messages: Messages) {

    private val config: YamlDocument? = configs.getConfig("language")

    private val useSeparator = config?.getBoolean("command.help.use-separator") ?: true

    @Command("dco", "discordo")
    @Subcommand("help")
    @Usage("dco help")
    fun onCommand(sender: CommandSender) {
        val commands: List<Pair<String, String>> = listOf(
            Pair("pvpm help", messages.getMessage("commands.help.description", sender)),
            Pair("pvpm reload", messages.getMessage("commands.reload.description", sender))
        )

        if (useSeparator) messages.sendMessage("commands.help.separator", sender)

        messages.sendMessage("commands.help.title", sender)

        for (command in commands) {
            messages.sendMessage("commands.help.line", sender,
                Pair("%command%", command.first),
                Pair("%command_description%", command.second)
            )
        }

        if (useSeparator) messages.sendMessage("commands.help.separator", sender)
    }

}