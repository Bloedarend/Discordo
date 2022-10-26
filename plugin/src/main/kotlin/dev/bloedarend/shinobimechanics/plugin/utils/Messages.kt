package dev.bloedarend.shinobimechanics.plugin.utils

import dev.dejvokep.boostedyaml.YamlDocument
import org.bukkit.ChatColor
import org.bukkit.entity.Player

class Messages(configs: Configs, private val modules: Modules) {

    private val config: YamlDocument? = configs.getConfig("language")

    fun sendMessage(path: String, player: Player, vararg placeholders: Pair<String, String>) {
        val message = config?.getString(path)
        val messages = config?.getList(path) ?: ArrayList<String>()
        if (message == null && messages.isEmpty()) return

        if (message != null && messages.isEmpty()) player.sendMessage(formatMessage(message, player, *placeholders))
        for (msg in messages) player.sendMessage(formatMessage(msg.toString(), player, *placeholders))
    }

    fun getMessage(path: String, player: Player, vararg placeholders: Pair<String, String>) : String {
        val message = config!!.getString(path)
        val messages = config.getList(path)

        var string = ""
        val strings = ArrayList<String>()

        if (message != null && messages.isEmpty()) string = formatMessage(message, player, *placeholders)
        for (msg in messages) strings.add("\n${formatMessage(msg.toString(), player, *placeholders)}")

        return string + strings.joinToString()
    }

    private fun formatMessage(msg: String, player: Player, vararg placeholders: Pair<String, String>) : String {
        val colorX = config!!.getChar("color-x")
        val colorY = config.getChar("color-y")
        val colorZ = config.getChar("color-z")
        val prefix = config.getString("prefix")

        var message = msg

        // Replace prefix with corresponding value.
        message = message.replace("%prefix%", prefix)

        // Replace color code placeholders with the corresponding colors.
        message = message.replace("&x", "&${colorX}")
            .replace("&y", "&${colorY}")
            .replace("&z", "&${colorZ}")

        // Replace player placeholders with the corresponding values.
        message = message.replace("%player_name%", player.name)
            .replace("%player_displayname%", player.displayName)

        // Replace additional placeholders.
        for (placeholder in placeholders) {
            message = message.replace(placeholder.first, placeholder.second)
        }

        return ChatColor.translateAlternateColorCodes('&', message)
    }

}