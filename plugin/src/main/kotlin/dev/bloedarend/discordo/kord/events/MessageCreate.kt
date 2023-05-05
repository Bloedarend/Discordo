package dev.bloedarend.discordo.kord.events

import com.vdurmont.emoji.EmojiParser
import dev.bloedarend.discordo.plugin.Main
import dev.bloedarend.discordo.plugin.utils.ConfigUtil
import dev.bloedarend.discordo.plugin.utils.HelperUtil
import dev.bloedarend.discordo.plugin.utils.MessageUtil
import dev.dejvokep.boostedyaml.YamlDocument
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class MessageCreate(private val plugin: Main) {

    private val discordo = plugin.discordo

    private val config = Config(ConfigUtil.getConfig("config"))

    suspend fun onMessageCreate(event: MessageCreateEvent) {
        val message = event.message
        val messageContents = ArrayList<Triple<String, HoverEvent?, ClickEvent?>>() // We have this, so we can split the message into components with different hover and click events.

        if (!config.enabled) return
        if (message.author?.isBot == true) return // We don't want to listen to bots.
        if (message.channelId != Snowflake(config.channelId)) return // We only want to listen to the defined channel in the configs.
        if (config.ignoreEmpty && message.content.isEmpty()) return

        // Don't send a message if it only contains emojis.
        if (config.removeEmotes && config.ignoreEmpty && EmojiParser.removeAllEmojis(message.content).replace(" ", "").replace(Regex("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>"), "").isEmpty()) return

        val member = message.getAuthorAsMember() ?: return
        val guild = member.guild.asGuild()

        var componentColor = Color(255, 255, 255)

        // Replace this, so role mentions won't be translated into colors.
        var content = message.content.replace("<@&", "<@&.")

        if (config.removeNewLine) {
            content = content.replace(Regex("[\\t\\n\\r]+"), " ") // Replace new line with space.
        }

        // Determine whether to translate color codes in the message or not.
        content =
            if (config.translateColorCodes) {
                org.bukkit.ChatColor.translateAlternateColorCodes('&', content).replace("<@&.", "<@&")
            } else {
                // Remove all codes from the message.
                content.replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), "")
            }

        // Translate or remove the emotes.
        if (config.emotesEnabled) {
            val emotePattern = Pattern.compile("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>") // Pattern for custom emojis.
            var emoteMatcher = emotePattern.matcher(content)
            while (emoteMatcher.find()) {
                content =
                    if (config.removeEmotes) {
                        // Remove emotes from the message.
                        content.replace(content.substring(emoteMatcher.start(), emoteMatcher.end()), "")
                    } else {
                        // Replace the emote with the name of the emote.
                        val emote = content.substring(emoteMatcher.start(), emoteMatcher.end())
                        content.replace(content.substring(emoteMatcher.start(), emoteMatcher.end()), emote.substring(emote.indexOf(':'), emote.lastIndexOf(':') + 1))
                    }

                emoteMatcher = emotePattern.matcher(content)
            }

            // Do the same for regular emojis.
            content =
                if (config.removeEmotes) {
                    EmojiParser.removeAllEmojis(content).replace(" ", "").replace(Regex("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>"), "")
                } else {
                    EmojiParser.parseToAliases(content)
                }
        }

        // Display and highlight mentions.
        if (config.mentionsEnabled) {
            val pattern = Pattern.compile("<((@(&.)?)|#)[0-9]{17,20}>") // Pattern for mentions
            var matcher = pattern.matcher(content)

            while (matcher.find()) {
                val mention = content.substring(matcher.start(), matcher.end())
                val preContent = content.substring(0, matcher.start()) // Text before the mention.

                if (mention.contains("@&.")) { // Mention is a role.
                    val role = guild.roles.firstOrNull {
                        it.id.value.toString() == mention.substring(4, mention.length - 1)
                    }

                    // Make sure the role exists.
                    if (role != null) {
                        // Add previous content to the message contents.
                        if (matcher.start() > 0) {
                            messageContents.add(Triple(preContent, null, null))
                        }

                        // Update content.
                        content = content.substring(matcher.end())

                        val roleColor = role.color
                        var newValue = "@${role.name}"

                        if (config.highlightMentions) {
                            val colorCode = getLastColorCode(preContent) ?: "&f"
                            val color: Color =
                                if (config.useRoleColor) {
                                    Color(roleColor.red, roleColor.green, roleColor.blue)
                                } else {
                                    HelperUtil.darkenColor(HelperUtil.getColor(colorCode), 1)
                                }
                            val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)
                            val styles = getLastStyleCodes(preContent)

                            // Add a highlight onto the mention.
                            newValue = "$highlightColorCode$styles$newValue"
                            content = "$colorCode$styles$content"
                        }

                        val hoverRole = MessageUtil.getMessage("discord.mentions.hover-role", null,
                            Pair("%role_name%", role.name),
                            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue))
                        )

                        val hoverEvent =
                            if (config.roleHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getHoverComponent(hoverRole, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        messageContents.add(Triple(newValue, hoverEvent, null))
                        matcher = pattern.matcher(content)
                    }
                } else if (mention.contains("@")) { // Mention is a member.
                    val mentionedMember = guild.members.firstOrNull {
                        it.id.value.toString() == mention.substring(2, mention.length - 1)
                    }

                    // Make sure the member exists.
                    if (mentionedMember != null) {
                        // Add previous content to the message contents.
                        if (matcher.start() > 0) {
                            messageContents.add(Triple(preContent, null, null))
                        }

                        // Update content.
                        content = content.substring(matcher.end())

                        var newValue = "@${mentionedMember.displayName}"
                        if (config.highlightMentions) {
                            val colorCode = getLastColorCode(preContent) ?: "&f"
                            val color = HelperUtil.darkenColor(HelperUtil.getColor(colorCode), 1)
                            val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)
                            val styles = getLastStyleCodes(preContent)

                            // Add a highlight onto the mention.
                            newValue = "$highlightColorCode$styles$newValue"
                            content = "$colorCode$styles$content"
                        }

                        val hoverMember = MessageUtil.getMessage("discord.mentions.hover-member", null, *getMemberPlaceholders(mentionedMember))

                        val hoverEvent =
                            if (config.memberHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getHoverComponent(hoverMember, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        val clickEvent =
                            if (config.memberClickEnabled) {
                                ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, mentionedMember.tag)
                            } else null

                        messageContents.add(Triple(newValue, hoverEvent, clickEvent))
                        matcher = pattern.matcher(content)
                    }
                } else if (mention.contains("#")) { // Mention is a channel.
                    val channel = guild.channels.firstOrNull {
                        it.id.value.toString() == mention.substring(2, mention.length - 1)
                    }

                    // Make sure the channel exists.
                    if (channel != null) {
                        // Add previous content to the message contents.
                        if (matcher.start() > 0) {
                            messageContents.add(Triple(preContent, null, null))
                        }

                        // Update content.
                        content = content.substring(matcher.end())

                        val isVoice = channel.type == ChannelType.GuildVoice || channel.type == ChannelType.GuildStageVoice
                        var icon = MessageUtil.getMessage("discord.mentions.icon-text-channel")
                        var path = "discord.mentions.hover-text-channel"

                        if (isVoice) {
                            icon = MessageUtil.getMessage("discord.mentions.icon-voice-channel")
                            path = "discord.mentions.hover-voice-channel"
                        }

                        var newValue = "${icon}${channel.name}"

                        if (config.highlightMentions) {
                            val colorCode = getLastColorCode(preContent) ?: "&f"
                            val color = HelperUtil.darkenColor(HelperUtil.getColor(colorCode), 1)
                            val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)
                            val styles = getLastStyleCodes(preContent)

                            // Add a highlight onto the mention.
                            newValue = "$highlightColorCode$styles$newValue"
                            content = "$colorCode$styles$content"
                        }

                        var connected = 0
                        guild.voiceStates.toList().forEach {
                            if (it.channelId == channel.id) connected++
                        }

                        val userLimit = channel.data.userLimit.asNullable ?: 0
                        val max =
                            if (userLimit <= 0) {
                                MessageUtil.getMessage("discord.mentions.icon-no-limit")
                            } else {
                                userLimit.toString()
                            }

                        val hoverChannel = MessageUtil.getMessage(path, null,
                            Pair("%channel_name%", channel.name),
                            Pair("%channel_description%", channel.data.topic.value ?: MessageUtil.getMessage("discord.mentions.no-channel-description")),
                            Pair("%channel_connected%", connected.toString()),
                            Pair("%channel_max%", max)
                        )

                        val hoverEvent =
                            if (!isVoice && config.textChannelHoverEnabled || isVoice && config.voiceChannelHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getHoverComponent(hoverChannel, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        val clickEvent =
                            if (!isVoice && config.textChannelClickEnabled || isVoice && config.voiceChannelClickEnabled) {
                                ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/channels/${guild.id.value}/${channel.id.value}")
                            } else null

                        messageContents.add(Triple(newValue, hoverEvent, clickEvent))
                        matcher = pattern.matcher(content)
                    }
                }
            }
        }

        // Add remainder of the content to the message contents.
        messageContents.add(Triple(content, null, null))

        var contentLength = 0
        var component = TextComponent("")
        val contentSplitForEvents = ArrayList<String>()
        var format = MessageUtil.getMessage("discord.format", null,
            Pair("%member_roles%", getMemberRoles(member).toList().joinToString(MessageUtil.getMessage("discord.member-roles.separator"))),
            Pair("%role_name%", getMemberRole(member)),
            Pair("%role_color%", getMemberColor(member))
        )

        // Split message for hover and click events.
        val pattern = Pattern.compile("%((member_name)|(member_displayname)|(member_tag)|(message))%")
        var matcher = pattern.matcher(format)

        while (matcher.find()) {
            if (matcher.start() > 0) {
                contentSplitForEvents.add(format.substring(0, matcher.start())) // Add previous string.
            }

            contentSplitForEvents.add(format.substring(matcher.start(), matcher.end())) // Add the match.

            format = format.substring(matcher.end())
            matcher = pattern.matcher(format)
        }

        contentSplitForEvents.add(format)

        // Update the component and give it hover and click events.
        contentSplitForEvents.forEach { string ->
            if (string.matches(Regex("%member_((name)|(displayname)|(tag))%"))) { // Match is supposed to have the main hover and click event.
                val milliseconds = message.timestamp.toEpochMilliseconds()
                val date = Date(milliseconds)
                val dateFormatted = SimpleDateFormat(config.dateFormat).format(date)

                val hoverMember = MessageUtil.getMessage("discord.hover", null, *getMemberPlaceholders(member))
                    .replace("%message_date%", dateFormatted)

                val hoverEvent =
                    if (config.mainHoverEnabled) {
                        val componentBuilder = ComponentBuilder().append(getHoverComponent(hoverMember, TextComponent(""))).create()
                        HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                    } else null

                val clickEvent =
                    if (config.mainClickEnabled) {
                        ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, member.tag)
                    } else null

                val newValue = when (string) {
                    "%member_name" -> {
                        member.username
                    }
                    "%member_displayname%" -> {
                        member.displayName
                    }
                    "%member_tag%" -> {
                        member.tag
                    }
                    else -> ""
                }

                val pair = getComponent(newValue, component, componentColor, hoverEvent, clickEvent)
                component = pair.first
                componentColor = pair.second
            } else if (string.matches(Regex("%message%"))) {
                // Replace message with content.
                messageContents.forEach { triple ->
                    if (contentLength < config.contentLimit) {
                        val currentLength = triple.first
                            .replace(ChatColor.COLOR_CHAR, '&')
                            .replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), "")
                            .length
                        val difference = config.contentLimit - (contentLength + currentLength)

                        if (difference >= 0) { // Adding the component does not exceed the content limit.
                            val pair = getComponent(triple.first, component, componentColor, triple.second, triple.third)
                            component = pair.first
                            componentColor = pair.second
                            contentLength += currentLength
                        } else { // Adding the component exceeds the content limit.
                            val newString = triple.first.substring(0, difference + triple.first.length) + "..."
                            contentLength = config.contentLimit

                            val pair = getComponent(newString, component, componentColor, triple.second, triple.third)
                            component = pair.first
                            componentColor = pair.second
                        }
                    }
                }
            } else { // Component is not a placeholder.
                val pair = getComponent(string, component, componentColor)
                component = pair.first
                componentColor = pair.second
            }
        }

        // Send the message to everyone one the server.
        plugin.server.spigot().broadcast(component)

        // Send the message to the console.
        if (config.sendToConsole) {
            plugin.server.consoleSender.spigot().sendMessage(component)
        }

        // Replace messages sent with images.
        if (config.replaceMessages) {
            message.delete()
            discordo.sendImage(component)
        }
    }

    private fun getComponent(string: String, textComponent: TextComponent, componentColor: Color, hoverEvent: HoverEvent? = null, clickEvent: ClickEvent? = null): Pair<TextComponent, Color> {
        var color = componentColor

        // The reset code will not default to white, but use the latest component color.
        // So here we'll replace it with '&f', to get the expected effect.
        var message = string.replace("&r", "&f").replace("&R", "&f")

        // Translate the message.
        message = ChatColor.translateAlternateColorCodes('&', message)

        val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
        var matcher = pattern.matcher(message)

        // Add a new component every time a hex code is found.
        while (matcher.find()) {
            val component = TextComponent(message.substring(0, matcher.start()))
            component.color = ChatColor.of(color)

            if (hoverEvent != null) {
                component.hoverEvent = hoverEvent
            }

            if (clickEvent != null) {
                component.clickEvent = clickEvent
            }

            textComponent.addExtra(component)

            color = HelperUtil.getColor(message.substring(matcher.start(), matcher.end()))
            message = message.substring(matcher.end())
            matcher = pattern.matcher(message)
        }

        // Add the remainder of the message.
        val component = TextComponent(message)
        component.color = ChatColor.of(color)

        val lastColorCode = getLastColorCode(message)
        if (lastColorCode != null) {
            color = HelperUtil.getColor(lastColorCode)
        }

        if (hoverEvent != null) {
            component.hoverEvent = hoverEvent
        }

        if (clickEvent != null) {
            component.clickEvent = clickEvent
        }

        textComponent.addExtra(component)

        return Pair(textComponent, color)
    }

    private fun getHoverComponent(string: String, textComponent: TextComponent): TextComponent {
        // The reset code will not default to white, but use the latest component color.
        // So here we'll replace it with '&f', to get the expected effect.
        var message = string.replace("&r", "&f").replace("&R", "&f")
        var color = Color(255, 255, 255)

        // Translate the message.
        message = ChatColor.translateAlternateColorCodes('&', message)

        val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
        var matcher = pattern.matcher(message)

        // Add a new component every time a hex code is found.
        while (matcher.find()) {
            val component = TextComponent(message.substring(0, matcher.start()))
            component.color = ChatColor.of(color)
            textComponent.addExtra(component)

            color = HelperUtil.getColor(message.substring(matcher.start(), matcher.end()))
            message = message.substring(matcher.end())
            matcher = pattern.matcher(message)
        }

        // Add the remainder of the message.
        val component = TextComponent(message)
        component.color = ChatColor.of(color)
        textComponent.addExtra(component)

        return textComponent
    }

    private suspend fun getMemberPlaceholders(member: Member): Array<Pair<String, String>> {
        return arrayOf(
            Pair("%member_name%", member.username),
            Pair("%member_displayname%", member.displayName),
            Pair("%member_tag%", member.tag),
            Pair("%member_roles%", getMemberRoles(member).toList().joinToString(MessageUtil.getMessage("discord.member-roles.separator"))),
            Pair("%role_name%", getMemberRole(member)),
            Pair("%role_color%", getMemberColor(member))
        )
    }

    private suspend fun getMemberRole(member: Member): String {
        val roles = member.roles.toList().sortedDescending()

        // Get the first hoisted role of the user.
        val hoistedRole = roles.firstOrNull {
            it.hoisted
        }

        return hoistedRole?.name ?: ""
    }

    private suspend fun getMemberRoles(member: Member): List<String> {
        val roles = member.roles.toList().sortedDescending()

        return roles.map {
            MessageUtil.getMessage("discord.member-roles.format", null,
                Pair("%current_role_name%", it.name),
                Pair("%current_role_color%", String.format("&#%02x%02x%02x", it.color.red, it.color.green, it.color.blue))
            )
        }
    }

    private suspend fun getMemberColor(member: Member): String {
        val roles = member.roles.toList().sortedDescending()

        // Get the first colored role of the user.
        val colorRole = roles.firstOrNull {
            it.color != dev.kord.common.Color(0x99AAB5)
        }

        val color = colorRole?.color ?: dev.kord.common.Color(170, 170, 170)

        return String.format("&#%02x%02x%02x", color.red, color.green, color.blue)
    }

    private fun getLastColorCode(string: String) : String? {
        val message = string.replace(ChatColor.COLOR_CHAR, '&')
        var code: String? = null

        val pattern = Pattern.compile("&(([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6}))")
        val matcher = pattern.matcher(message)

        while (matcher.find()) {
            code = message.substring(matcher.start(), matcher.end())
        }

        return code
    }

    private fun getLastStyleCodes(string: String): String {
        var message = string.replace(ChatColor.COLOR_CHAR, '&')
        var styles = ""

        val colorPattern = Pattern.compile("&(([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6}))")
        var colorMatcher = colorPattern.matcher(message)

        while (colorMatcher.find()) {
            message = message.substring(colorMatcher.end()) // Update the message till the last color code has been found.
            colorMatcher = colorPattern.matcher(message)
        }

        val stylePattern = Pattern.compile("&(k|K|l|L|m|M|n|N|o|O)")
        val styleMatcher = stylePattern.matcher(message)

        while (styleMatcher.find()) {
            styles += message.substring(styleMatcher.start(), styleMatcher.end()) // Add style code to styles.
        }

        return styles
    }

    data class Config(private val config: YamlDocument?) {
        val channelId = config?.getString("channel-id") ?: ""

        val enabled = config?.getBoolean("discord.enabled") ?: true
        val sendToConsole = config?.getBoolean("discord.send-to-console") ?: true
        val translateColorCodes = config?.getBoolean("discord.translate-color-codes") ?: false
        val removeNewLine = config?.getBoolean("discord.remove-new-line") ?: true
        val contentLimit = config?.getInt("discord.content-limit") ?: 512
        val ignoreEmpty = config?.getBoolean("discord.ignore-empty") ?: true
        val replaceMessages = config?.getBoolean("discord.replace-messages") ?: false
        val dateFormat = config?.getString("discord.date-format") ?: "d MMMM yyyy, hh:mm:ss"
        val mentionsEnabled = config?.getBoolean("discord.mentions.enabled") ?: true
        val highlightMentions = config?.getBoolean("discord.mentions.highlight") ?: true
        val mainHoverEnabled = config?.getBoolean("discord.hover") ?: true
        val mainClickEnabled = config?.getBoolean("discord.click") ?: true
        val memberHoverEnabled = config?.getBoolean("discord.mentions.member.hover") ?: true
        val memberClickEnabled = config?.getBoolean("discord.mentions.member.click") ?: true
        val roleHoverEnabled = config?.getBoolean("discord.mentions.role.hover") ?: true
        val useRoleColor = config?.getBoolean("discord.mentions.role.use-role-color") ?: false
        val textChannelHoverEnabled = config?.getBoolean("discord.mentions.text-channel.hover") ?: true
        val textChannelClickEnabled = config?.getBoolean("discord.mentions.text-channel.click") ?: true
        val voiceChannelHoverEnabled = config?.getBoolean("discord.mentions.voice-channel.hover") ?: true
        val voiceChannelClickEnabled = config?.getBoolean("discord.mentions.voice-channel.click") ?: true
        val emotesEnabled = config?.getBoolean("discord.emotes.enabled") ?: true
        val removeEmotes = config?.getBoolean("discord.emotes.remove") ?: false
    }
}