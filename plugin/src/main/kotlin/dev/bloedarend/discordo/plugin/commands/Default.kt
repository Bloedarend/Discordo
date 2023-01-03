package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Usage

class Default(configs: Configs, private val messages: Messages, private val plugin: Plugin) {

    private val config: YamlDocument? = configs.getConfig("language")

    private val useSeparator = config?.getBoolean("commands.default.use-separator") ?: true

    @Command("dco", "discordo")
    @Default
    @Usage("dco help")
    fun onCommand(sender: CommandSender) {
        val information: List<Pair<String, String>> = listOf(
            Pair("Version", plugin.description.version),
            Pair("Author", plugin.description.authors.joinToString(", "))
        )

        if (useSeparator) messages.sendMessage("commands.default.separator", sender)

        messages.sendMessage("commands.default.title", sender)

        for (info in information) {
            messages.sendMessage("commands.default.line", sender,
                Pair("%property%", info.first),
                Pair("%value%", info.second)
            )
        }

        if (useSeparator) messages.sendMessage("commands.default.separator", sender)
    }
}