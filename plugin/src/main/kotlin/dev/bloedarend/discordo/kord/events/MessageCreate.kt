package dev.bloedarend.discordo.kord.events

import com.vdurmont.emoji.EmojiParser
import dev.bloedarend.discordo.kord.Bot
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Helpers
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.plugin.Plugin
import java.util.regex.Pattern

class MessageCreate(private val plugin: Plugin, configs: Configs, private val messages: Messages, private val helpers: Helpers) {

    private val config = configs.getConfig("config")
    private val channelId = Snowflake(config?.getString("channel-id") ?: "")

    private val enabled = config?.getBoolean("discord.enabled") ?: true
    private val translateColorCodes = config?.getBoolean("discord.translate-color-codes") ?: false
    private val mentionsEnabled = config?.getBoolean("discord.mentions.enabled") ?: true
    private val highlightMentions = config?.getBoolean("discord.mentions.highlight") ?: true
    private val emotesEnabled = config?.getBoolean("discord.emotes.enabled") ?: true
    private val removeEmotes = config?.getBoolean("discord.emotes.remove") ?: false

    suspend fun onMessageCreate(event: MessageCreateEvent) {
        val message = event.message

        if (!enabled) return
        if (message.author?.isBot == true) return // We don't want to listen to bots.
        if (message.channelId != channelId) return // We only want to listen to the defined channel in the configs.

        // Don't send a message if it only contains emojis.
        if (removeEmotes && EmojiParser.removeAllEmojis(message.content).isEmpty()) return

        val member = message.getAuthorAsMember() ?: return
        val guild = member.guild.asGuild()
        val roles = member.roles.toList().sortedDescending()

        // TODO add default role color

        // TODO separate click and hover events
        // TODO add hover and click event for attachments
        // TODO add hover for mention information

        // Get the first colored role of the user.
        val colorRole = roles.firstOrNull {
            it.color != Color(0x99AAB5)
        }

        // Get the first hoisted role of the user.
        val hoistedRole = roles.firstOrNull {
            it.hoisted
        }

        val roleColor = colorRole?.color ?: Color(170, 170, 170)
        val roleName = hoistedRole?.name ?:
            if (roles.isEmpty()) ""
            else roles.first().name

        val memberRoles = roles.map {
            messages.getMessage("discord.member-roles.format", null,
                Pair("%current_role_name%", it.name),
                Pair("%current_role_color%", String.format("&#%02x%02x%02x", it.color.red, it.color.green, it.color.blue))
            )
        }

        val placeholders = arrayOf(
            Pair("%member_name%", member.username),
            Pair("%member_displayname%", member.displayName),
            Pair("%member_tag%", member.tag),
            Pair("%member_roles%", memberRoles.toList().joinToString(messages.getMessage("discord.member-roles.separator"))),
            Pair("%role_name%", roleName),
            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue))
        )

        val components = getComponents("discord.format", message.content, guild, *placeholders)
        val hover = getComponents("discord.hover", message.content, guild, *placeholders)
        val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(hover))
        val clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/channels/${message.getGuild().id.value}/${message.channelId.value}/${message.id.value}")

        components.forEach {
            it.hoverEvent = hoverEvent
            it.clickEvent = clickEvent
        }

        plugin.server.spigot().broadcast(*components)
    }

    private suspend fun getComponents(path: String, content: String, guild: Guild, vararg placeholders: Pair<String, String>): Array<BaseComponent> {
        var message = messages.getMessage(path, null, *placeholders)
        var messageContent = content

        // For some reason the reset code will not reset the message, but use the latest hex code.
        // So here we'll replace it with '&f', to get the expected effect.
        messageContent = messageContent.replace("&r", "&f")

        // Translate the message.
        message =
            if (translateColorCodes) message.replace("%message%", org.bukkit.ChatColor.translateAlternateColorCodes('&', messageContent))
            else {
                // Remove all codes from the message.
                message.replace("%message%", messageContent.replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), ""))
            }

        if (mentionsEnabled) {
            val memberPattern = Pattern.compile("<@[0-9]{17,20}>") // Pattern for discord members.
            var memberMatcher = memberPattern.matcher(messageContent)
            while(memberMatcher.find()) {
                val member = guild.members.firstOrNull {
                    it.id.value.toString() == messageContent.substring(memberMatcher.start() + 2, memberMatcher.end() - 1)
                }

                // Make sure the member exists.
                if (member != null) {
                    var newValue = "@${member.displayName}"

                    if (highlightMentions) {
                        val colorCode = getLastColorCode(messageContent.substring(0, memberMatcher.start()))
                        val color = helpers.darkenColor(helpers.getColor(colorCode), 1)
                        val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)

                        // Add a highlight onto the mention.
                        newValue = "$highlightColorCode$newValue$colorCode"
                    }

                    messageContent = messageContent.replace(messageContent.substring(memberMatcher.start(), memberMatcher.end()), newValue)
                    memberMatcher = memberPattern.matcher(messageContent)
                }
            }

            val rolePattern = Pattern.compile("<@&[0-9]{17,20}>") // Pattern for guild roles.
            var roleMatcher = rolePattern.matcher(messageContent)
            while (roleMatcher.find()) {
                val role = guild.roles.firstOrNull {
                    it.id.value.toString() == messageContent.substring(roleMatcher.start() + 3, roleMatcher.end() -1)
                }

                // Make sure the role exists.
                if (role != null) {
                    messageContent = messageContent.replace(messageContent.substring(roleMatcher.start(), roleMatcher.end()), "@${role.name}")
                    roleMatcher = rolePattern.matcher(messageContent)
                }
            }

            val channelPattern = Pattern.compile("<#[0-9]{17,20}>") // Pattern for text and voice channels.
            var channelMatcher = channelPattern.matcher(messageContent)
            while (channelMatcher.find()) {
                val channel = guild.channels.firstOrNull {
                    it.id.value.toString() == messageContent.substring(channelMatcher.start() + 2, channelMatcher.end() - 1)
                }

                // Make sure the channel exists.
                if (channel != null) {
                    val icon =
                        if (channel.type == ChannelType.GuildVoice || channel.type == ChannelType.GuildStageVoice) "♪" // Channel is voice.
                        else "#" // Channel is text.

                    messageContent = messageContent.replace(messageContent.substring(channelMatcher.start(), channelMatcher.end()), "${icon}${channel.name}")
                    channelMatcher = channelPattern.matcher(messageContent)
                }
            }

            val emotePattern = Pattern.compile("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>") // Pattern for custom emojis.
            var emoteMatcher = emotePattern.matcher(messageContent)
            while (emoteMatcher.find()) {
                if (removeEmotes) {
                    // Remove emotes from the message.
                    messageContent = messageContent.replace(messageContent.substring(emoteMatcher.start(), emoteMatcher.end()), "")
                } else if (emotesEnabled) {
                    // Replace the emote with the name of the emote.
                    val emote = messageContent.substring(emoteMatcher.start(), emoteMatcher.end())
                    messageContent = messageContent.replace(messageContent.substring(emoteMatcher.start(), emoteMatcher.end()), emote.substring(emote.indexOf(':'), emote.lastIndexOf(':') + 1))
                }

                emoteMatcher = emotePattern.matcher(messageContent)
            }
        }

        // Do the same for regular emojis.
        if (removeEmotes) {
            messageContent = EmojiParser.removeAllEmojis(messageContent)
        } else if (emotesEnabled) {
            messageContent = EmojiParser.parseToAliases(messageContent)
        }

        val componentBuilder = ComponentBuilder()

        val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
        var matcher = pattern.matcher(message)
        var color = ""

        while (matcher.find()) {
            componentBuilder.append(message.substring(0, matcher.start()))
            if (color.isNotEmpty()) componentBuilder.currentComponent.color = ChatColor.of(color)

            color = message.substring(matcher.start() + 1, matcher.end())
            message = message.substring(matcher.end())
            matcher = pattern.matcher(message)
        }

        componentBuilder.append(message)
        if (color.isNotEmpty()) componentBuilder.currentComponent.color = ChatColor.of(color)

        return componentBuilder.create()
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
}