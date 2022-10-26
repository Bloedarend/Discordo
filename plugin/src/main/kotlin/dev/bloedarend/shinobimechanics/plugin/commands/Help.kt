package dev.bloedarend.shinobimechanics.plugin.commands

import dev.bloedarend.shinobimechanics.plugin.utils.Configs
import dev.bloedarend.shinobimechanics.plugin.utils.Messages
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Usage

class Help(configs: Configs, private val messages: Messages) {

    private val config: YamlDocument? = configs.getConfig("language")

    private val useSeparator = config?.getBoolean("command.help.use-separator") ?: true

    @Command("sm", "shinobim", "shinobimechanics")
    @Subcommand("help")
    @Usage("sm help <category>")
    fun onCommand(player: Player) {
        val commands: List<Pair<String, String>> = listOf(
            Pair("pvpm help", messages.getMessage("commands.help.description", player)),
            Pair("pvpm reload", messages.getMessage("commands.reload.description", player))
        )

        if (useSeparator) messages.sendMessage("commands.help.separator", player)

        messages.sendMessage("commands.help.title", player)

        for (command in commands) {
            messages.sendMessage("commands.help.line", player,
                Pair("%command%", command.first),
                Pair("%command_description%", command.second)
            )
        }

        if (useSeparator) messages.sendMessage("commands.help.separator", player)
    }

}