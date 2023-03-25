package dev.bloedarend.discordo.plugin.utils

import org.bukkit.plugin.Plugin
import org.imgscalr.Scalr
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.regex.Pattern
import javax.imageio.ImageIO

class Images(plugin: Plugin, configs: Configs, private val helpers: Helpers) {

    private val config = configs.getConfig("config")

    private val textShadowEnabled = config?.getBoolean("minecraft.text-shadow.enabled") ?: true
    private val textShadowDarkness = config?.getInt("minecraft.text-shadow.darkness") ?: 3
    private val spacing = config?.getInt("minecraft.image.spacing") ?: 3
    private val padding = config?.getInt("minecraft.image.padding") ?: 8
    private val width = config?.getInt("minecraft.image.width") ?: 900
    private val backgroundOpacity = config?.getFloat("minecraft.image.background-opacity") ?: 0.4F
    private val backgroundColor = Color(0F, 0F, 0F, if (backgroundOpacity < 0) 0F else if (backgroundOpacity > 1) 1F else backgroundOpacity)

    private val fontSize = 30F
    private val height = (fontSize + padding * 2).toInt()
    private val maxStringWidth = width - 2 * padding

    private val fontRegular = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftRegular-Bmg3.ttf")).deriveFont(fontSize)
    private val fontBold = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftBold-nMK1.ttf")).deriveFont(fontSize)
    private val fontItalic = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftItalic-R8Mo.ttf")).deriveFont(fontSize)
    private val fontBoldItalic = Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("MinecraftBoldItalic-1y1e.ttf")).deriveFont(fontSize)

    fun getInputStream(message: String): InputStream {
        var currentFont = fontRegular
        var currentColor = Color.WHITE
        var currentWidth = 0
        var currentStyles = ""
        var currentLine = ""
        val lines = ArrayList<String>()

        var image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var g2d = image.createGraphics()

        initGraphics2D(image.createGraphics(), currentFont, currentColor, image)

        val words = message.split(" ")
        words.forEach {
            val stringWidth = getStringWidth(g2d, "$it ")

            // Check if the text object should be split up, go to the next line or remain on the same line.
            if (stringWidth > maxStringWidth) {
                // The string is too long to fit onto one line. Add the current line to the lines.
                lines.add(currentLine)

                val characters = it.toCharArray()
                var style = ""
                var newTextFormatted = " "

                // Loop over every character in the string to check where the string should be split up.
                characters.forEach { character ->
                    val textWidth = getStringWidth(g2d, newTextFormatted)
                    val characterWidth = getStringWidth(g2d, character.toString())

                    if (textWidth + characterWidth > maxStringWidth) {
                        // The string is too big to fit onto one line. Add the current text to the lines.
                        lines.add(newTextFormatted)
                        newTextFormatted = " "
                    }

                    // Check if the character array contains the start of a code.
                    if (character == '&') {
                        // The character is an '&', which indicates a potential start of a code.
                        style = character.toString()
                    } else if (style.contains("#")) {
                        // The style contains a '#', which means we should look for a combination of 6 valid hex code characters.
                        if (character.toString().matches(Regex("[0-9a-fA-F]"))) {
                            // The current character could be part of a hex code.
                            if (style.length == 7) {
                                // The current style is of length 7. By adding the current character, the hex code will be complete.
                                newTextFormatted += "$style$character"
                                style = ""
                            } else {
                                // The hex code is not complete.
                                style += character
                            }
                        } else {
                            // The character is not a valid hex code character, so add the style + character to the string.
                            newTextFormatted += "$style$character"
                            style = ""
                        }
                    } else if (style.contains("&")) {
                        // The style contains a '&', which means we should look to complete this code.
                        if (character.toString().matches(Regex("[a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O"))) {
                            // A regular Minecraft code was found.
                            newTextFormatted += "&$character"
                            style = ""
                        } else if (character == '#') {
                            // The start of a potential hex code was found.
                            style += character
                        } else {
                            // The code was not completed.
                            newTextFormatted += style
                            style = ""
                        }
                    } else {
                        // The character has nothing to do with codes, so add it to the string.
                        newTextFormatted += character
                    }

                    // Set the remaining text to the current line.
                    currentLine = newTextFormatted
                }
            } else if (stringWidth + currentWidth < maxStringWidth) {
                // The text will be able to fit on the line just fine.
                currentWidth += stringWidth
                currentLine += "$it "
            } else {
                // The text won't fit on the current line, so put it onto the next one.
                currentWidth = getStringWidth(g2d , " $it ")
                lines.add(currentLine)
                currentLine = " $it "
            }
        }

        // Add the remaining line to the lines.
        lines.add(currentLine)
        image = resize(image, lines.size)
        g2d = image.createGraphics()

        // Create new graphics for the new image.
        initGraphics2D(g2d, currentFont, currentColor, image)

        // Loop over every line and draw it.
        lines.forEachIndexed { index, it ->
            var line = it
            var position = 0

            val pattern = Pattern.compile("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))") // Regex for every color code and hex color code.
            var matcher = pattern.matcher(line)

            // Look for a code.
            while (matcher.find()) {
                val text = line.substring(0, matcher.start()) // The text before the code.
                val code = line.substring(matcher.start(), matcher.end())

                // Draw the string, then update the position.
                drawText(g2d, text, index, position, currentColor, currentStyles)
                position += g2d.fontMetrics.stringWidth(text)

                // Check if the code represents a color.
                if (code.matches(Regex("&(([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6}))"))) {
                    // After every color code, the styles are reset.
                    currentStyles = ""
                    currentColor = helpers.getColor(code)
                    g2d.color = currentColor
                }

                // Check if the code represents a style.
                if (code.matches(Regex("&(k|K|l|L|m|M|n|N|o|O)"))) {
                    currentStyles += code.substring(1)
                }

                // Set properties for the next string.
                currentFont = getFont(currentStyles)
                g2d.font = currentFont

                // Update the line and matcher.
                line = line.substring(matcher.end())
                matcher = pattern.matcher(line)
            }

            // Draw the remaining string.
            drawText(g2d, line.replace(Regex("\\s+$"), ""), index, position, currentColor, currentStyles)
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        image.flush()

        return ByteArrayInputStream(baos.toByteArray())
    }

    private fun drawText(g2d: Graphics2D, text: String, index: Int, position: Int, currentColor: Color, currentStyles: String) {
        val stringWidth = g2d.fontMetrics.stringWidth(text)
        val textShadowOffset = (fontSize / 10).toInt()

        val stringX = padding + position
        val stringY = (padding + fontSize + index * (fontSize + spacing) - fontSize / 6).toInt()

        var lineXStart = (position + fontSize / 7.5).toInt()
        val lineXEnd = (position + stringWidth + fontSize / 10).toInt()
        val nLineY = (padding + fontSize + index * (fontSize + spacing) - fontSize / 15).toInt()
        val mLineY = (padding + fontSize + index * (fontSize + spacing) - fontSize / 2).toInt()

        // Lines with an index bigger than 0 will start with a space. We do not want the line to be drawn on this space.
        if (index > 0 && position == 0) {
            lineXStart += g2d.fontMetrics.stringWidth(text.toCharArray()[0].toString())
        }

        // Draw the text shadow.
        if (textShadowEnabled) {
            g2d.color = helpers.darkenColor(currentColor, textShadowDarkness)
            g2d.drawString(text, stringX + textShadowOffset, stringY + textShadowOffset)

            if (currentStyles.lowercase().contains("n")) {
                g2d.drawLine(lineXStart + textShadowOffset, nLineY + textShadowOffset, lineXEnd + textShadowOffset, nLineY + textShadowOffset)
            }

            if (currentStyles.lowercase().contains("m")) {
                g2d.drawLine(lineXStart + textShadowOffset, mLineY + textShadowOffset, lineXEnd + textShadowOffset, mLineY + textShadowOffset)
            }

            g2d.color = currentColor
        }

        // Draw the formatted string.
        g2d.drawString(text, stringX, stringY)

        // Draw underline.
        if (currentStyles.lowercase().contains("n")) {
            g2d.drawLine(lineXStart, nLineY, lineXEnd, nLineY)
        }

        // Draw strikethrough
        if (currentStyles.lowercase().contains("m")) {
            g2d.drawLine(lineXStart, mLineY, lineXEnd, mLineY)
        }
    }

    private fun resize(image: BufferedImage, lineAmount: Int): BufferedImage {
        image.flush()

        println(lineAmount)
        println(fontSize)
        println(padding)
        println(spacing)
        println(fontSize.toInt() + padding * 2 + (lineAmount - 1) * (fontSize + spacing).toInt())

        // Rescale the image according to the amount of lines.
        return Scalr.resize(image, Scalr.Mode.FIT_EXACT, width, fontSize.toInt() + padding * 2 + (lineAmount - 1) * (fontSize + spacing).toInt())
    }

    private fun getStringWidth(g2d: Graphics2D , string: String): Int {
        var text = string
        var styles = ""
        var stringWidth = 0

        val pattern = Pattern.compile("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))") // Regex for every color code and hex color code.
        var matcher = pattern.matcher(text)

        // Look for a code.
        while (matcher.find()) {
            val subtext = text.substring(0, matcher.start()) // The text before the code.
            val code = text.substring(matcher.start(), matcher.end())

            // Reset style if a color code was found.
            if (code.matches(Regex("&(([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6}))"))) {
                styles = ""
            }

            // Update style if it's a style.
            if (code.matches(Regex("&(k|K|l|L|m|M|n|N|o|O)"))) {
                styles += code.substring(1)
            }

            // Increment string width and then update the font for the next string.
            stringWidth += g2d.fontMetrics.stringWidth(subtext)
            g2d.font = getFont(styles)

            // Update the text and the matcher.
            text = text.substring(matcher.end())
            matcher = pattern.matcher(text)
        }

        // Add remaining width to the string width.
        stringWidth += g2d.fontMetrics.stringWidth(text)

        return stringWidth
    }

    private fun getFont(styles: String): Font {
        val isBold = styles.lowercase().contains("l")
        val isItalic = styles.lowercase().contains("o")

        return if (isBold && isItalic) fontBoldItalic
            else if (isBold) fontBold
            else if (isItalic) fontItalic
            else fontRegular
    }

    private fun initGraphics2D(g2d: Graphics2D, currentFont: Font, currentColor: Color, image: BufferedImage) {
        g2d.font = currentFont
        g2d.color = currentColor
        g2d.background = backgroundColor
        g2d.stroke = BasicStroke(3F)
        g2d.clearRect(0, 0, image.width, image.height)
    }

}