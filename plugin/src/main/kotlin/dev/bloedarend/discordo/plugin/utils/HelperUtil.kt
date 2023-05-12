package dev.bloedarend.discordo.plugin.utils

import dev.bloedarend.discordo.plugin.Main
import org.bukkit.plugin.Plugin
import java.awt.Color
import java.awt.Font
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class HelperUtil private constructor() {

    companion object {
        fun getVersion(plugin: Main): Int {
            // Get the first two values of the minecraft version.
            val version: Matcher = Pattern.compile("\\d+\\.\\d+").matcher(plugin.server.version)
            version.find()

            // Get the second integer of the minecraft version. This will be easier to compare in code later on.
            val comparableVersion: Matcher = Pattern.compile("\\d+").matcher(version.group())
            comparableVersion.find(1)

            return comparableVersion.group().toInt()
        }

        fun getColor(colorCode: String) : Color {
            val hex = (if (colorCode.matches(Regex("&([a-fA-F0-9]|r|R)"))) convertColorCodeToHex(colorCode) else colorCode)?.substring(1)

            return Color.decode(hex)
        }

        private fun convertColorCodeToHex(colorCode: String): String? {
            return when (colorCode.lowercase()) {
                "&0" -> "&#000000"
                "&1" -> "&#0000aa"
                "&2" -> "&#00aa00"
                "&3" -> "&#00aaaa"
                "&4" -> "&#aa0000"
                "&5" -> "&#aa00aa"
                "&6" -> "&#ffaa00"
                "&7" -> "&#aaaaaa"
                "&8" -> "&#555555"
                "&9" -> "&#5555ff"
                "&a" -> "&#55ff55"
                "&b" -> "&#55ffff"
                "&c" -> "&#ff5555"
                "&d" -> "&#ff55ff"
                "&e" -> "&#ffff55"
                "&f" -> "&#ffffff"
                "&r" -> "&#ffffff"
                else -> null
            }
        }

        fun darkenColor(color: Color, amount: Int): Color {
            if (amount <= 1) return color.darker()

            return darkenColor(color.darker(), amount - 1)
        }

        fun testFonts(plugin: Plugin) {
            val config = ConfigUtil.getConfig("config")
            val useFontPack = config?.getBoolean("minecraft.image.use-font-pack") ?: true

            try {
                if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftRegular-Bmg3.ttf"))
                else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf"))
            } catch (exception: IOException) {
                plugin.logger.warning("Font regular could not be found, using default font!")
            }

            try {
                if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftBold-nMK1.ttf"))
                else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf"))
            } catch (exception: IOException) {
                plugin.logger.warning("Font bold could not be found, using default font!")
            }

            try {
                if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftItalic-R8Mo.ttf"))
                else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf"))
            } catch (exception: IOException) {
                plugin.logger.warning("Font italic could not be found, using default font!")
            }

            try {
                if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftBoldItalic-1y1e.ttf"))
                else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf"))
            } catch (exception: IOException) {
                plugin.logger.warning("Font bold-italic could not be found, using default font!")
            }
        }

    }

}