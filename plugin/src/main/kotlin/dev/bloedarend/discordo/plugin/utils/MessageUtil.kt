package dev.bloedarend.discordo.plugin.utils

import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.regex.Pattern

class MessageUtil private constructor() {

    companion object {
        private var config = Config(ConfigUtil.getConfig("language"))

        fun sendMessage(path: String, sender: CommandSender, vararg placeholders: Pair<String, String>) {
            val message = config.language?.getString(path)
            val messages = config.language?.getList(path) ?: ArrayList<String>()
            if (message == null && messages.isEmpty()) return

            val player = if (sender is Player) sender else null

            if (message != null && messages.isEmpty()) sender.sendMessage(formatHexMessage(formatMessage(message, player, *placeholders)))
            for (msg in messages) sender.sendMessage(formatHexMessage(formatMessage(msg.toString(), player, *placeholders)))
        }

        fun getMessage(path: String, sender: CommandSender? = null, vararg placeholders: Pair<String, String>): String {
            val message = config.language!!.getString(path)
            val messages = config.language!!.getList(path)

            val player = if (sender is Player) sender else null

            return if (message != null && messages.isEmpty()) formatMessage(message, player, *placeholders)
            else messages.map { formatMessage(it as String, player, *placeholders) }.toList().joinToString("\n")
        }

        private fun formatMessage(msg: String, player: Player?, vararg placeholders: Pair<String, String>): String {
            var message = msg

            // Replace prefix with corresponding value.
            message = message.replace("%prefix%", config.prefix)

            // Replace color code placeholders with the corresponding colors.
            message = message.replace("&x", "&${config.colorX}")
                .replace("&y", "&${config.colorY}")
                .replace("&z", "&${config.colorZ}")

            // Replace player placeholders with the corresponding values.
            if (player != null) {
                message = message.replace("%player_name%", player.name)
                    .replace("%player_displayname%", player.displayName)
            }

            // Replace additional placeholders.
            for (placeholder in placeholders) {
                message = message.replace(placeholder.first, placeholder.second)
            }

            return ChatColor.translateAlternateColorCodes('&', message)
        }

        private fun formatHexMessage(msg: String): String {
            var message = msg
            val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
            var matcher = pattern.matcher(message)

            while (matcher.find()) {
                val color = message.substring(matcher.start(), matcher.end())
                message = message.replace(color, "${net.md_5.bungee.api.ChatColor.of(color.substring(1))}")
                matcher = pattern.matcher(message)
            }

            return message
        }

        fun reload() {
            config = Config(ConfigUtil.getConfig("language"))
        }

        data class Config(private val config: YamlDocument?) {
            val language = config
            val colorX = config?.getChar("color-x") ?: '8'
            val colorY = config?.getChar("color-y") ?: '7'
            val colorZ = config?.getChar("color-z") ?: 'f'
            val prefix = config?.getString("prefix") ?: "●"
        }
    }

}