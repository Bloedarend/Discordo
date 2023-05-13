package dev.bloedarend.discordo.plugin.utils

import dev.bloedarend.discordo.plugin.Main
import java.awt.Color
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
            val hex = (if (colorCode.matches(Regex("&([a-fA-F0-9]|r|R)"))) convertColorCodeToHex(colorCode) else colorCode).substring(1)

            return Color.decode(hex)
        }

        fun convertColorCodeToHex(colorCode: String): String {
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
                else -> colorCode
            }
        }

        fun convertHexToRGB(hex: String): Triple<Int, Int, Int> {
            val r = Integer.valueOf(hex.substring(1, 3), 16)
            val g = Integer.valueOf(hex.substring(3, 5), 16)
            val b = Integer.valueOf(hex.substring(5, 7), 16)

            return Triple(r, g, b)
        }

        fun darkenColor(color: Color, amount: Int): Color {
            if (amount <= 1) return color.darker()

            return darkenColor(color.darker(), amount - 1)
        }
    }


}