package dev.bloedarend.discordo.plugin.commands

import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Usage

class Default(private val plugin: Plugin) {

    private val config: YamlDocument? = ConfigUtil.getConfig("language")

    private val useSeparator = config?.getBoolean("commands.default.use-separator") ?: true

    @Command("dco", "discordo")
    @Default
    @Usage("dco help")
    fun onCommand(sender: CommandSender) {
        val information: List<Pair<String, String>> = listOf(
            Pair("Version", plugin.description.version),
            Pair("Author", plugin.description.authors.joinToString(", "))
        )

        if (useSeparator) MessageUtil.sendMessage("commands.default.separator", sender)

        MessageUtil.sendMessage("commands.default.title", sender)

        for (info in information) {
            MessageUtil.sendMessage("commands.default.line", sender,
                Pair("%property%", info.first),
                Pair("%value%", info.second)
            )
        }

        if (useSeparator) MessageUtil.sendMessage("commands.default.separator", sender)
    }
}