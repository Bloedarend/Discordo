package dev.bloedarend.discordo.kord.events

import com.vdurmont.emoji.EmojiParser
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Helpers
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.common.toMessageFormat
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
import org.bukkit.plugin.Plugin
import java.awt.Color
import java.util.regex.Pattern

class MessageCreate(private val plugin: Plugin, configs: Configs, private val messages: Messages, private val helpers: Helpers) {

    private val config = configs.getConfig("config")
    private val channelId = Snowflake(config?.getString("channel-id") ?: "")

    private val enabled = config?.getBoolean("discord.enabled") ?: true
    private val translateColorCodes = config?.getBoolean("discord.translate-color-codes") ?: false
    private val mentionsEnabled = config?.getBoolean("discord.mentions.enabled") ?: true
    private val highlightMentions = config?.getBoolean("discord.mentions.highlight") ?: true
    private val mainHoverEnabled = config?.getBoolean("discord.hover") ?: true
    private val mainClickEnabled = config?.getBoolean("discord.click") ?: true
    private val memberHoverEnabled = config?.getBoolean("discord.mentions.member.hover") ?: true
    private val memberClickEnabled = config?.getBoolean("discord.mentions.member.click") ?: true
    private val roleHoverEnabled = config?.getBoolean("discord.mentions.role.hover") ?: true
    private val channelHoverEnabled = config?.getBoolean("discord.mentions.channel.hover") ?: true
    private val channelClickEnabled = config?.getBoolean("discord.mentions.channel.click") ?: true
    private val emotesEnabled = config?.getBoolean("discord.emotes.enabled") ?: true
    private val removeEmotes = config?.getBoolean("discord.emotes.remove") ?: false

    private var componentColor = Color(255, 255, 255)
    private var componentStyles = ""

    suspend fun onMessageCreate(event: MessageCreateEvent) {
        val message = event.message
        val messageSplit = ArrayList<Triple<String, HoverEvent?, ClickEvent?>>() // We have this, so we can split the message into components with different hover and click events.

        if (!enabled) return
        if (message.author?.isBot == true) return // We don't want to listen to bots.
        if (message.channelId != channelId) return // We only want to listen to the defined channel in the configs.

        // Don't send a message if it only contains emojis.
        if (removeEmotes && EmojiParser.removeAllEmojis(message.content).isEmpty()) return

        val member = message.getAuthorAsMember() ?: return
        val guild = member.guild.asGuild()

        // Reset the component properties.
        componentColor = Color(255, 255, 255)
        componentStyles = ""

        // Determine whether to translate color codes in the message or not.
        var content =
            if (translateColorCodes) {
                org.bukkit.ChatColor.translateAlternateColorCodes('&', message.content)
            } else {
                // Remove all codes from the message.
                message.content.replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), "")
            }

        // Translate or remove the emotes.
        if (emotesEnabled) {
            val emotePattern = Pattern.compile("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>") // Pattern for custom emojis.
            var emoteMatcher = emotePattern.matcher(content)
            while (emoteMatcher.find()) {
                content =
                    if (removeEmotes) {
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
                if (removeEmotes) {
                    EmojiParser.removeAllEmojis(content)
                } else {
                    EmojiParser.parseToAliases(content)
                }
        }

        var contentToSplit = content
        var index = 0

        // Display and highlight mentions.
        if (mentionsEnabled) {
            val pattern = Pattern.compile("<((@#?)|#)[0-9]{17,20}>") // Pattern for mentions
            var matcher = pattern.matcher(contentToSplit)

            while (matcher.find()) {
                val mention = content.substring(matcher.start(), matcher.end())

                if (mention.contains("@#")) { // Mention is a role.
                    val role = guild.roles.firstOrNull {
                        it.id.value.toString() == content.substring(matcher.start() + 3, matcher.end() -1)
                    }

                    // Make sure the role exists.
                    if (role != null) {
                        val newValue = "@${role.name}"

                        content = content.replace(content.substring(index + matcher.start(), index + matcher.end()), newValue)
                        index += matcher.end()

                        if (matcher.start() > 0) {
                            messageSplit.add(Triple(contentToSplit.substring(0, matcher.start()), null, null))
                        }

                        val color = role.color
                        val hoverRole = messages.getMessage("discord.mentions.hover-role", null, *arrayOf(
                            Pair("%role_name%", role.name),
                            Pair("%role_color%", String.format("&#%02x%02x%02x", color.red, color.green, color.blue))
                        ))

                        val hoverEvent =
                            if (roleHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getComponent(hoverRole, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        messageSplit.add(Triple(newValue, hoverEvent, null))

                        contentToSplit = contentToSplit.substring(matcher.end())
                        matcher = pattern.matcher(contentToSplit)
                    }
                } else if (mention.contains("@")) { // Mention is a member.
                    val mentionedMember = guild.members.firstOrNull {
                        it.id.value.toString() == content.substring(matcher.start() + 2, matcher.end() - 1)
                    }

                    // Make sure the member exists.
                    if (mentionedMember != null) {
                        var newValue = "@${mentionedMember.displayName}"

                        if (highlightMentions) {
                            val colorCode = getLastColorCode(content.substring(0, matcher.start()))
                            val color = helpers.darkenColor(helpers.getColor(colorCode), 1)
                            val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)

                            // Add a highlight onto the mention.
                            newValue = "$highlightColorCode$newValue$colorCode"
                        }

                        content = content.replace(content.substring(index + matcher.start(), index + matcher.end()), newValue)
                        index += matcher.end()

                        if (matcher.start() > 0) {
                            messageSplit.add(Triple(contentToSplit.substring(0, matcher.start()), null, null))
                        }

                        val hoverMember = messages.getMessage("discord.mentions.hover-member", null, *getMemberPlaceholders(mentionedMember))

                        val hoverEvent =
                            if (memberHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getComponent(hoverMember, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        val clickEvent =
                            if (memberClickEnabled) {
                                ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, mentionedMember.tag)
                            } else null

                        messageSplit.add(Triple(newValue, hoverEvent, clickEvent))

                        contentToSplit = contentToSplit.substring(matcher.end())
                        matcher = pattern.matcher(contentToSplit)
                    }
                } else if (mention.contains("#")) { // Mention is a channel.
                    val channel = guild.channels.firstOrNull {
                        it.id.value.toString() == content.substring(matcher.start() + 2, matcher.end() - 1)
                    }

                    // Make sure the channel exists.
                    if (channel != null) {
                        val icon =
                            if (channel.type == ChannelType.GuildVoice || channel.type == ChannelType.GuildStageVoice) "♪" // Channel is voice.
                            else "#" // Channel is text.

                        val newValue = "${icon}${channel.name}"

                        content = content.replace(content.substring(index + matcher.start(), index + matcher.end()), newValue)
                        index += matcher.end()

                        if (matcher.start() > 0) {
                            messageSplit.add(Triple(contentToSplit.substring(0, matcher.start()), null, null))
                        }

                        val hoverChannel = messages.getMessage("discord.mentions.hover-channel", null, *arrayOf(
                            Pair("%channel_name", channel.name),
                            Pair("%channel_description", channel.data.topic.value ?: "Empty")
                        ))

                        val hoverEvent =
                            if (channelHoverEnabled) {
                                val componentBuilder = ComponentBuilder().append(getComponent(hoverChannel, TextComponent(""))).create()
                                HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                            } else null

                        val clickEvent =
                            if (channelClickEnabled) {
                                ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/channels/${guild.id.value}/${channel.id.value}")
                            } else null

                        messageSplit.add(Triple(newValue, hoverEvent, clickEvent))

                        contentToSplit = contentToSplit.substring(matcher.end())
                        matcher = pattern.matcher(contentToSplit)
                    }
                }
            }
        }

        messageSplit.add(Triple(contentToSplit, null, null))

        var component = TextComponent("")
        val roleColor = getMemberColor(member)
        val formatSplit = ArrayList<String>()
        var format = messages.getMessage("discord.format", null,
            Pair("%member_roles%", getMemberRoles(member).toList().joinToString(messages.getMessage("discord.member-roles.separator"))),
            Pair("%role_name%", getMemberRole(member)),
            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue))
        )

        // Split message for hover and click events.
        val pattern = Pattern.compile("%((member_name)|(member_displayname)|(member_tag)|(message))%")
        var matcher = pattern.matcher(format)

        while (matcher.find()) {
            if (matcher.start() > 0) {
                formatSplit.add(format.substring(0, matcher.start())) // Add previous string.
            }

            formatSplit.add(format.substring(matcher.start(), matcher.end())) // Add the match.

            format = format.substring(matcher.end())
            matcher = pattern.matcher(format)
        }

        formatSplit.add(format)

        // Update the component builder and give it hover and click events.
        formatSplit.forEach { string ->
            if (string.matches(Regex("%member_((name)|(displayname)|(tag))%"))) { // Give member hover.
                val hoverMember = messages.getMessage("discord.hover", null, *getMemberPlaceholders(member))
                    .replace("%message_date%", message.timestamp.toMessageFormat(DiscordTimestampStyle.LongDateTime))

                val hoverEvent =
                    if (mainHoverEnabled) {
                        val componentBuilder = ComponentBuilder().append(getComponent(hoverMember, TextComponent(""))).create()
                        HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(componentBuilder))
                    } else null

                val clickEvent =
                    if (mainClickEnabled) {
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

                component = getComponent(newValue, component, hoverEvent, clickEvent)
            } else if (string.matches(Regex("%message%"))) { // Replace message with content.
                messageSplit.forEach { triple ->
                    component = getComponent(triple.first, component, triple.second, triple.third)
                }
            } else {
                component = getComponent(string, component)
            }
        }

        // Send the message to everyone one the server.
        plugin.server.spigot().broadcast(component)
    }

    private fun getComponent(string: String, textComponent: TextComponent, hoverEvent: HoverEvent? = null, clickEvent: ClickEvent? = null) : TextComponent {
        // The reset code will not default to white, but use the latest component color.
        // So here we'll replace it with '&f', to get the expected effect.
        var message = componentStyles + string.replace("&r", "&f").replace("&R", "&f")

        val pattern = Pattern.compile("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))")
        var matcher = pattern.matcher(message)

        // Add a new component every time a hex code is found.
        while (matcher.find()) {
            val component = TextComponent(message.substring(0, matcher.start()))

            component.color = ChatColor.of(componentColor)

            if (hoverEvent != null) {
                component.hoverEvent = hoverEvent
            }

            if (clickEvent != null) {
                component.clickEvent = clickEvent
            }

            textComponent.addExtra(component)

            componentColor = helpers.getColor(message.substring(matcher.start(), matcher.end()))
            message = message.substring(matcher.end())
            matcher = pattern.matcher(message)
        }

        // Add the remainder of the message.
        val component = TextComponent(message)

        component.color = ChatColor.of(componentColor)
        componentColor = helpers.getColor(getLastColorCode(message))
        componentStyles = getLastStyleCodes(string)

        if (hoverEvent != null) {
            component.hoverEvent = hoverEvent
        }

        if (clickEvent != null) {
            component.clickEvent = clickEvent
        }

        textComponent.addExtra(component)

        return textComponent
    }

    private suspend fun getMemberPlaceholders(member: Member) : Array<Pair<String, String>> {
        val roleColor = getMemberColor(member)

        return arrayOf(
            Pair("%member_name%", member.username),
            Pair("%member_displayname%", member.displayName),
            Pair("%member_tag%", member.tag),
            Pair("%member_roles%", getMemberRoles(member).toList().joinToString(messages.getMessage("discord.member-roles.separator"))),
            Pair("%role_name%", getMemberRole(member)),
            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue))
        )
    }

    private suspend fun getMemberRole(member: Member) : String {
        val roles = member.roles.toList().sortedDescending()

        // Get the first hoisted role of the user.
        val hoistedRole = roles.firstOrNull {
            it.hoisted
        }

        return hoistedRole?.name ?: ""
    }

    private suspend fun getMemberRoles(member: Member) : List<String> {
        val roles = member.roles.toList().sortedDescending()

        return roles.map {
            messages.getMessage("discord.member-roles.format", null,
                Pair("%current_role_name%", it.name),
                Pair("%current_role_color%", String.format("&#%02x%02x%02x", it.color.red, it.color.green, it.color.blue))
            )
        }
    }

    private suspend fun getMemberColor(member: Member) : dev.kord.common.Color {
        val roles = member.roles.toList().sortedDescending()

        // Get the first colored role of the user.
        val colorRole = roles.firstOrNull {
            it.color != dev.kord.common.Color(0x99AAB5)
        }

        return colorRole?.color ?: dev.kord.common.Color(170, 170, 170)

    }

    private fun getLastColorCode(string: String) : String {
        val pattern = Pattern.compile("&(([a-fA-F0-9]|r|R)|(#[a-fA-F0-9]{6}))")
        val matcher = pattern.matcher(string)

        var code = "&f"

        while (matcher.find()) {
            code = string.substring(matcher.start(), matcher.end())
        }

        return code
    }

    private fun getLastStyleCodes(string: String) : String {
        var message = string
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
}