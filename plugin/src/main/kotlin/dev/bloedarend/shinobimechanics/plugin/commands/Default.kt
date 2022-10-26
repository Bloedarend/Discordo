package dev.bloedarend.shinobimechanics.plugin.commands

import dev.bloedarend.shinobimechanics.plugin.utils.Configs
import dev.bloedarend.shinobimechanics.plugin.utils.Messages
import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Usage

class Default(configs: Configs, private val messages: Messages, private val plugin: Plugin) {

    private val config: YamlDocument? = configs.getConfig("language")

    private val useSeparator = config?.getBoolean("commands.default.use-separator") ?: true

    @Command("sm", "shinobim", "shinobimechanics")
    @Default
    @Usage("sm help")
    fun onCommand(player: Player) {
        val information: List<Pair<String, String>> = listOf(
            Pair("Version", plugin.description.version),
            Pair("Author", plugin.description.authors.joinToString(", "))
        )

        if (useSeparator) messages.sendMessage("commands.default.separator", player)

        messages.sendMessage("commands.default.title", player)

        for (info in information) {
            messages.sendMessage("commands.default.line", player,
                Pair("%property%", info.first),
                Pair("%value%", info.second)
            )
        }

        if (useSeparator) messages.sendMessage("commands.default.separator", player)
    }
}