package dev.bloedarend.discordo.plugin

import dev.bloedarend.discordo.api.DiscordoAPI
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.bloedarend.discordo.plugin.utils.HelperUtil
import dev.dejvokep.boostedyaml.YamlDocument
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import io.ktor.client.request.forms.*
import io.ktor.util.cio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import org.imgscalr.Scalr
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception
import java.lang.NumberFormatException
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import javax.imageio.ImageIO

open class Discordo(private val plugin: Main) : DiscordoAPI {

    private var config = Config(ConfigUtil.getConfig("config"), plugin)
    lateinit var scope: CoroutineScope

    override fun sendImage(string: String): CompletableFuture<Unit> = scope.future {
        createMessage(getInputStream(string), config.channelId)
    }

    override fun sendImage(string: String, channelId: String) = scope.future {
        createMessage(getInputStream(string), channelId)
    }

    override fun sendImage(textComponent: TextComponent) = scope.future {
        createMessage(getInputStream(convertToString(textComponent)), config.channelId)
    }

    override fun sendImage(textComponent: TextComponent, channelId: String) = scope.future {
        createMessage(getInputStream(convertToString(textComponent)), channelId)
    }

    private suspend fun createMessage(inputStream: InputStream, channelId: String) {
        val client = plugin.bot.client ?: return
        val guildSnowflake: Snowflake?
        val channelSnowflake: Snowflake?

        try {
            guildSnowflake = Snowflake(config.guildId)
            channelSnowflake = Snowflake(channelId)
        } catch (exception: NumberFormatException) {
            throw Exception("The guild id '${config.guildId}' or the channel id '$channelId' are incorrect!")
        }

        if (config.guildId.isEmpty() || channelId.isEmpty()) return

        val guild = client.getGuildOrNull(guildSnowflake) ?: throw Exception("Guild with guildId '${config.guildId}' could not be found.")
        val channel = guild.getChannelOfOrNull<TextChannel>(channelSnowflake) ?: throw Exception("Channel with channelId '${channelId}' could not be found.")

        channel.createMessage {
            val provider = ChannelProvider {
                inputStream.toByteReadChannel()
            }

            addFile("discordo.png", provider)
        }
    }

    private fun convertToString(textComponent: TextComponent): String {
        var legacyMessage = textComponent.toLegacyText().replace(ChatColor.COLOR_CHAR, '&')

        val legacyPattern = Pattern.compile("&x(&[0-9a-fA-F]){6}")
        var legacyMatcher = legacyPattern.matcher(legacyMessage)

        // Look for legacy hex code and replace it with our format.
        while (legacyMatcher.find()) {
            val match = legacyMessage.substring(legacyMatcher.start(), legacyMatcher.end())
            val hexCode = "&#${match[3]}${match[5]}${match[7]}${match[9]}${match[11]}${match[13]}"

            legacyMessage = legacyMessage.replaceFirst(match, hexCode)
            legacyMatcher = legacyPattern.matcher(legacyMessage)
        }

        return legacyMessage
    }

    private fun getStringWidth(g2d: Graphics2D, string: String): Int {
        var string = string
        var styles = ""
        var stringWidth = 0

        val pattern = Pattern.compile("&((${config.colorRegex}|${config.styleRegex})|(${config.hexRegex}))")
        var matcher = pattern.matcher(string)

        // Look for a code.
        while (matcher.find()) {
            val preString = string.substring(0, matcher.start()) // The text before the code.
            val code = string.substring(matcher.start(), matcher.end())

            // Reset style if a color code was found.
            if (code.matches(Regex("&((${config.colorRegex})|(${config.hexRegex}))"))) {
                styles = ""
            }

            // Update style if it's a style.
            if (code.matches(Regex("&(${config.styleRegex})"))) {
                styles += code.substring(1)
            }

            // Increment string width and then update the font for the next string.
            stringWidth += g2d.fontMetrics.stringWidth(preString)
            g2d.font = getFont(styles)

            // Update the text and the matcher.
            string = string.substring(matcher.end())
            matcher = pattern.matcher(string)
        }

        // Add remaining width to the string width.
        stringWidth += g2d.fontMetrics.stringWidth(string)

        return stringWidth
    }

    private fun getFont(styles: String): Font {
        val isBold = styles.lowercase().contains("l")
        val isItalic = styles.lowercase().contains("o")

        return if (isBold && isItalic) config.fontBoldItalic
        else if (isBold) config.fontBold
        else if (isItalic) config.fontItalic
        else config.fontRegular
    }

    private fun loadGraphics2D(g2d: Graphics2D, currentFont: Font, currentColor: Color, image: BufferedImage) {
        g2d.font = currentFont
        g2d.color = currentColor
        g2d.background = config.backgroundColor
        g2d.stroke = BasicStroke(3F)
        g2d.clearRect(0, 0, image.width, image.height)
    }

    private fun resize(image: BufferedImage, lineAmount: Int): BufferedImage {
        image.flush()

        // Rescale the image according to the amount of lines.
        return Scalr.resize(image, Scalr.Mode.FIT_EXACT, config.width, (config.height + (lineAmount - 1) * (config.fontSize + config.spacing)).toInt())
    }

    private fun getInputStream(string: String): InputStream {
        var currentFont = config.fontRegular
        var currentColor = Color.WHITE
        var currentWidth = 0
        var currentStyles = ""
        var currentLine = ""
        val lines = ArrayList<String>()

        var image = BufferedImage(config.width, config.height, BufferedImage.TYPE_INT_ARGB)
        var g2d = image.createGraphics()

        loadGraphics2D(g2d, currentFont, currentColor, image)

        // Split the string into words and split it up into multiple lines.
        val words = string.split(" ")
        words.forEach { word ->
            val stringWidth = getStringWidth(g2d, "$word ")

            if (stringWidth > config.maxStringWidth) { // The string is too long to fit onto one line.
                // Add the current line to the lines.
                lines.add(currentLine)

                val characters = word.toCharArray()
                var styles = ""
                var newLine = " "

                // Loop over every character in the word to check where the string should be split up.
                characters.forEach { character ->
                    val lineWidth = getStringWidth(g2d, newLine)
                    val characterWidth = getStringWidth(g2d, character.toString())

                    if (lineWidth + characterWidth > config.maxStringWidth) { // No more characters can be added to the new line.
                        // Add the new line to the lines.
                        lines.add(newLine)
                        newLine = " "
                    }

                    // Check if the character array contains the start of a code.
                    if (character == '&') { // The character is an '&', which indicates a potential start of a code.
                        styles = character.toString()
                    } else if (styles.contains("#")) { // The style contains a '#', which means we should look for a combination of 6 valid hex code characters.
                        if (character.toString().matches(Regex(config.colorRegex))) {
                            if (styles.length == 7) { // The current style is of length 7. By adding the current character, the hex code will be complete.
                                newLine += "$styles$character"
                                styles = ""
                            } else { // The hex code is not complete.
                                styles += character
                            }
                        } else {
                            // Add the style + character to the new line, as there is no complete hex code..
                            newLine += "$styles$character"
                            styles = ""
                        }
                    } else if (styles.contains("&")) {
                        if (character.toString().matches(Regex("${config.colorRegex}|${config.styleRegex}"))) { // A regular Minecraft code was found.
                            newLine += "&$character"
                            styles = ""
                        } else if (character == '#') { // The start of a potential hex code was found.
                            styles += character
                        } else { // The code was not a color or hex code.
                            newLine += styles
                            styles = ""
                        }
                    } else { // The character has nothing to do with codes, so add it to the string.
                        newLine += character
                    }

                    // Set the remaining text to the current line.
                    currentLine = newLine
                }
            } else if (stringWidth + currentWidth < config.maxStringWidth) { // The text will be able to fit on the line just fine.
                currentWidth += stringWidth
                currentLine += "$word "
            } else { // The text won't fit on the current line.
                // Go to the next line.
                currentWidth = getStringWidth(g2d , " $word ")
                lines.add(currentLine)
                currentLine = " $word "
            }
        }

        // Add the remaining line to the lines.
        lines.add(currentLine)
        image = resize(image, lines.size)
        g2d = image.createGraphics()

        // Load new graphics for the new image.
        loadGraphics2D(g2d, currentFont, currentColor, image)

        // Loop over every line and draw it.
        lines.forEachIndexed { index, it ->
            var line = it
            var position = 0

            val pattern = Pattern.compile("&((${config.colorRegex}|${config.styleRegex})|(${config.hexRegex}))")
            var matcher = pattern.matcher(line)

            // Look for a code.
            while (matcher.find()) {
                val preLine = line.substring(0, matcher.start()) // The text before the code.
                val code = line.substring(matcher.start(), matcher.end())

                // Draw the previous part of the line, then update the position.
                drawText(g2d, preLine, index, position, currentColor, currentStyles)
                position += g2d.fontMetrics.stringWidth(preLine)

                // Check if the code represents a color.
                if (code.matches(Regex("&((${config.colorRegex})|(${config.hexRegex}))"))) {
                    // After every color code, the styles are reset.
                    currentStyles = ""
                    currentColor = HelperUtil.getColor(code)
                    g2d.color = currentColor
                }

                // Check if the code represents a style.
                if (code.matches(Regex("&(${config.styleRegex})"))) {
                    currentStyles += code.substring(1)
                }

                // Set properties for the next string.
                currentFont = getFont(currentStyles)
                g2d.font = currentFont

                // Update the line and matcher.
                line = line.substring(matcher.end())
                matcher = pattern.matcher(line)
            }

            // Draw the remainder of the line.
            drawText(g2d, line.replace(Regex("\\s+$"), ""), index, position, currentColor, currentStyles)
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        image.flush()

        return ByteArrayInputStream(baos.toByteArray())
    }

    private fun drawText(g2d: Graphics2D, string: String, index: Int, position: Int, currentColor: Color, currentStyles: String) {
        val stringWidth = g2d.fontMetrics.stringWidth(string)
        val textShadowOffset = (config.fontSize / 10).toInt()

        val stringX = config.padding + position
        val stringY = (config.padding + config.fontSize + index * (config.fontSize + config.spacing) - config.fontSize / 6).toInt()

        var lineXStart = (position + config.fontSize / 7.5).toInt()
        val lineXEnd = (position + stringWidth + config.fontSize / 10).toInt()
        val nLineY = (config.padding + config.fontSize + index * (config.fontSize + config.spacing) - config.fontSize / 15).toInt()
        val mLineY = (config.padding + config.fontSize + index * (config.fontSize + config.spacing) - config.fontSize / 2).toInt()

        // Lines with an index bigger than 0 will start with a space. We do not want the line to be drawn on this space.
        if (index > 0 && position == 0) {
            lineXStart += g2d.fontMetrics.stringWidth(string.toCharArray()[0].toString())
        }

        // Draw the text shadow.
        if (config.textShadowEnabled) {
            g2d.color = HelperUtil.darkenColor(currentColor, config.textShadowDarkness)
            g2d.drawString(string, stringX + textShadowOffset, stringY + textShadowOffset)

            if (currentStyles.lowercase().contains("n")) {
                g2d.drawLine(lineXStart + textShadowOffset, nLineY + textShadowOffset, lineXEnd + textShadowOffset, nLineY + textShadowOffset)
            }

            if (currentStyles.lowercase().contains("m")) {
                g2d.drawLine(lineXStart + textShadowOffset, mLineY + textShadowOffset, lineXEnd + textShadowOffset, mLineY + textShadowOffset)
            }

            g2d.color = currentColor
        }

        // Draw the formatted string.
        g2d.drawString(string, stringX, stringY)

        // Draw underline.
        if (currentStyles.lowercase().contains("n")) {
            g2d.drawLine(lineXStart, nLineY, lineXEnd, nLineY)
        }

        // Draw strikethrough
        if (currentStyles.lowercase().contains("m")) {
            g2d.drawLine(lineXStart, mLineY, lineXEnd, mLineY)
        }
    }

    fun reload() {
        config = Config(ConfigUtil.getConfig("config"), plugin)
    }

    data class Config(private val config: YamlDocument?, private val plugin: Main) {
        val textShadowEnabled = config?.getBoolean("minecraft.text-shadow.enabled") ?: true
        val textShadowDarkness = config?.getInt("minecraft.text-shadow.darkness") ?: 3
        val useFontPack = config?.getBoolean("minecraft.image.use-font-pack") ?: true
        val spacing = config?.getInt("minecraft.image.spacing") ?: 3
        val padding = config?.getInt("minecraft.image.padding") ?: 8
        val width = config?.getInt("minecraft.image.width") ?: 900
        val backgroundOpacity = config?.getFloat("minecraft.image.background-opacity") ?: 0.4F
        val backgroundColor = Color(0F, 0F, 0F, if (backgroundOpacity < 0) 0F else if (backgroundOpacity > 1) 1F else backgroundOpacity)

        val fontSize = 30F
        val height = (fontSize + padding * 2).toInt()
        val maxStringWidth = width - 2 * padding

        val fontRegular =
            if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftRegular-Bmg3.ttf")).deriveFont(fontSize)
            else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf")).deriveFont(fontSize * 0.8F)
        val fontBold =
            if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftBold-nMK1.ttf")).deriveFont(fontSize)
            else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf")).deriveFont(fontSize * 0.8F).deriveFont(Font.BOLD)
        val fontItalic =
            if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftItalic-R8Mo.ttf")).deriveFont(fontSize)
            else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf")).deriveFont(fontSize * 0.8F).deriveFont(Font.ITALIC)
        val fontBoldItalic =
            if (useFontPack) Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/MinecraftBoldItalic-1y1e.ttf")).deriveFont(fontSize)
            else Font.createFont(Font.TRUETYPE_FONT, plugin.getResource("fonts/F77MinecraftRegular-0VYv.ttf")).deriveFont(fontSize * 0.8F).deriveFont(Font.BOLD or Font.ITALIC)

        val guildId = config?.getString("guild-id") ?: ""
        val channelId = config?.getString("channel-id") ?: ""

        val colorRegex = "[0-9a-fA-F]|r|R"
        val hexRegex = "#[0-9a-fA-F]{6}"
        val styleRegex = "k|K|l|L|m|M|n|N|o|O"
    }

}