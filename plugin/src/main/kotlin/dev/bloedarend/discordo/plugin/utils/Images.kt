package dev.bloedarend.discordo.plugin.utils

import org.bukkit.plugin.Plugin
import org.imgscalr.Scalr
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.regex.Pattern
import javax.imageio.ImageIO

class Images(private val plugin: Plugin, private val messages: Messages) {

    private val padding = 3
    private val fontSize = 30F
    private val height = (fontSize + padding * 2).toInt()
    private val width = (fontSize * 30).toInt()
    private val maxStringWidth = width - 2 * padding

    private val backgroundColor = Color(0F, 0F, 0F, 0.4F)
    private var image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private var g2d: Graphics2D = image.createGraphics()

    private val fontRegular = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftRegular-Bmg3.ttf")).deriveFont(fontSize)
    private val fontBold = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftBold-nMK1.ttf")).deriveFont(fontSize)
    private val fontItalic = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftItalic-R8Mo.ttf")).deriveFont(fontSize)
    private val fontBoldItalic = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftBoldItalic-1y1e.ttf")).deriveFont(fontSize)

    private var currentFont = fontBold
    private var currentColor = Color.WHITE
    private var currentStyles = ""
    private var currentWidth = 0
    private var currentPosition = 0

    private val colorCodeRegex = "&([a-fA-F0-9]|r|R)"
    private val colorRegex = "&([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6})"
    private val styleRegex = "&(k|K|l|L|m|M|n|N|o|O)"

    private var line = ArrayList<ArrayList<Triple<String, String, Color>>>()
    private val lines = ArrayList<ArrayList<ArrayList<Triple<String, String, Color>>>>()

    init {
        g2d.font = currentFont
        g2d.color = currentColor
        g2d.background = backgroundColor
        g2d.clearRect(0, 0, image.width, image.height)
    }

    fun getInputStream(message: String) : InputStream {
        val words = message.split(" ")

        // Reset all properties
        currentFont = fontRegular
        currentColor = Color.WHITE
        currentWidth = 0
        currentPosition = 0
        line.clear()
        lines.clear()
        resize()

        words.forEach {
            val textObjects = ArrayList<Triple<String, String, Color>>()
            var word = it
            var stringWidth = 0

            val pattern = Pattern.compile("&([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6})") // Regex for every color code and hex color code.
            var matcher = pattern.matcher(word)

            while (matcher.find()) {
                val text = word.substring(0, matcher.start()) // The text before the code.
                val code = word.substring(matcher.start(), matcher.end())

                // The code that was found represents a color.
                if (code.matches(Regex("&([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6})"))) {
                    // After every color code, the styles are reset.
                    currentStyles = ""
                    currentColor = getColor(code)
                }

                // The code that was found represents a style.
                if (code.matches(Regex("&(k|K|l|L|m|M|n|N|o|O)"))) {
                    currentStyles += code.substring(1)
                }

                stringWidth += g2d.fontMetrics.stringWidth(text)
                textObjects.add(Triple(text, currentStyles, currentColor))

                currentFont = getFont(currentStyles)
                g2d.font = currentFont

                word = word.substring(matcher.end())
                matcher = pattern.matcher(word)
            }

            stringWidth += g2d.fontMetrics.stringWidth(word)
            textObjects.add(Triple("$word ", currentStyles, currentColor))
            addWord(textObjects, stringWidth)
        }

        currentFont = fontRegular
        g2d.font = currentFont

        lines.add(line)
        resize()

        lines.forEachIndexed{ index, line ->
            var position = if (index > 0) (fontSize / 2).toInt() else 0
            line.forEach{ word ->
                word.forEach { textObject ->
                    g2d.drawString(textObject.first, padding + position, ((index + 1) * (fontSize + padding) - fontSize / 6).toInt())
                    position += g2d.fontMetrics.stringWidth(textObject.first)

                    currentFont = getFont(textObject.second)
                    currentColor = textObject.third

                    g2d.font = currentFont
                    g2d.color = currentColor
                }
            }
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        image.flush()

        return ByteArrayInputStream(baos.toByteArray())
    }

    private fun resize() {
        image.flush()
        g2d.dispose()

        // Rescale the image according to the amount of lines.
        image = Scalr.resize(image, Scalr.Mode.FIT_EXACT, width, padding + lines.size * (fontSize + padding).toInt())
        g2d = image.createGraphics()

        g2d.font = currentFont
        g2d.color = currentColor
        g2d.background = backgroundColor
        g2d.clearRect(0, 0, image.width, image.height)
    }

    private fun addWord(textObjects: ArrayList<Triple<String, String,Color>>, stringWidth: Int) {
        println(stringWidth)
        println(currentWidth)
        // Check if the text object should be split up, go to the next line or remain on the same line.
        if (stringWidth > maxStringWidth) {
            // LOGIC FOR TOO LONG STRING
        } else if (stringWidth + currentWidth < maxStringWidth) {
            line.add(textObjects)
        } else {
            lines.add(line)
            line = ArrayList()
            line.add(textObjects)
            currentWidth = 0
        }

        currentWidth += stringWidth
    }

    private fun getColor(colorCode: String) : Color {
        val hex = (if (colorCode.matches(Regex(colorCodeRegex))) convertColorCodeToHex(colorCode) else colorCode)?.substring(1)

        return Color.decode(hex)
    }

    private fun convertColorCodeToHex(colorCode: String) : String? {
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

    private fun getFont(styles: String) : Font {
        val isBold = styles.lowercase().contains("l")
        val isItalic = styles.lowercase().contains("o")

        return if (isBold && isItalic) fontBoldItalic
            else if (isBold) fontBold
            else if (isItalic) fontItalic
            else fontRegular
    }

}