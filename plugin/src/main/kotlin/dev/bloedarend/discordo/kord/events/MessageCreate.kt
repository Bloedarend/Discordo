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

        var content =
            if (translateColorCodes) {
                org.bukkit.ChatColor.translateAlternateColorCodes('&', message.content)
            } else {
                // Remove all codes from the message.
                message.content.replace(Regex("&(([a-fA-F0-9]|r|R|k|K|l|L|m|M|n|N|o|O)|(#[a-fA-F0-9]{6}))"), "")
            }

        if (mentionsEnabled) {
            val memberPattern = Pattern.compile("<@[0-9]{17,20}>") // Pattern for discord members.
            var memberMatcher = memberPattern.matcher(content)
            while(memberMatcher.find()) {
                val mentionedMember = guild.members.firstOrNull {
                    it.id.value.toString() == content.substring(memberMatcher.start() + 2, memberMatcher.end() - 1)
                }

                // Make sure the member exists.
                if (mentionedMember != null) {
                    var newValue = "@${mentionedMember.displayName}"

                    if (highlightMentions) {
                        val colorCode = getLastColorCode(content.substring(0, memberMatcher.start()))
                        val color = helpers.darkenColor(helpers.getColor(colorCode), 1)
                        val highlightColorCode = String.format("&#%02x%02x%02x", color.red, color.green, color.blue)

                        // Add a highlight onto the mention.
                        newValue = "$highlightColorCode$newValue$colorCode"
                    }

                    content = content.replace(content.substring(memberMatcher.start(), memberMatcher.end()), newValue)
                    memberMatcher = memberPattern.matcher(content)
                }
            }

            val rolePattern = Pattern.compile("<@&[0-9]{17,20}>") // Pattern for guild roles.
            var roleMatcher = rolePattern.matcher(content)
            while (roleMatcher.find()) {
                val role = guild.roles.firstOrNull {
                    it.id.value.toString() == content.substring(roleMatcher.start() + 3, roleMatcher.end() -1)
                }

                // Make sure the role exists.
                if (role != null) {
                    content = content.replace(content.substring(roleMatcher.start(), roleMatcher.end()), "@${role.name}")
                    roleMatcher = rolePattern.matcher(content)
                }
            }

            val channelPattern = Pattern.compile("<#[0-9]{17,20}>") // Pattern for text and voice channels.
            var channelMatcher = channelPattern.matcher(content)
            while (channelMatcher.find()) {
                val channel = guild.channels.firstOrNull {
                    it.id.value.toString() == content.substring(channelMatcher.start() + 2, channelMatcher.end() - 1)
                }

                // Make sure the channel exists.
                if (channel != null) {
                    val icon =
                        if (channel.type == ChannelType.GuildVoice || channel.type == ChannelType.GuildStageVoice) "♪" // Channel is voice.
                        else "#" // Channel is text.

                    content = content.replace(content.substring(channelMatcher.start(), channelMatcher.end()), "${icon}${channel.name}")
                    channelMatcher = channelPattern.matcher(content)
                }
            }

            val emotePattern = Pattern.compile("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>") // Pattern for custom emojis.
            var emoteMatcher = emotePattern.matcher(content)
            while (emoteMatcher.find()) {
                if (removeEmotes) {
                    // Remove emotes from the message.
                    content = content.replace(content.substring(emoteMatcher.start(), emoteMatcher.end()), "")
                } else if (emotesEnabled) {
                    // Replace the emote with the name of the emote.
                    val emote = content.substring(emoteMatcher.start(), emoteMatcher.end())
                    content = content.replace(content.substring(emoteMatcher.start(), emoteMatcher.end()), emote.substring(emote.indexOf(':'), emote.lastIndexOf(':') + 1))
                }

                emoteMatcher = emotePattern.matcher(content)
            }
        }

        // Do the same for regular emojis.
        if (removeEmotes) {
            content = EmojiParser.removeAllEmojis(content)
        } else if (emotesEnabled) {
            content = EmojiParser.parseToAliases(content)
        }

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
            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue)),
            Pair("%message%", content)
        )

        var format = messages.getMessage("discord.format", null, *placeholders)
        val hover = messages.getMessage("discord.hover", null, *placeholders)

        // Split message on hover and click events.
        val formats = ArrayList<String>()
        val pattern = Pattern.compile("%((member_name)|(member_displayname)|(member_tag)|(message))%")
        var matcher = pattern.matcher(format)

        while (matcher.find()) {
            if (matcher.start() > 0) {
                formats.add(format.substring(0, matcher.start())) // Add previous string.
            }

            formats.add(format.substring(matcher.start(), matcher.end())) // Add the match.

            format = format.substring(matcher.end())
            matcher = pattern.matcher(format)
        }

        formats.add(format)





        // TODO use pair list to differentiate between mention and attachment and other stuff.

        val components = ArrayList<BaseComponent>()



        val prefixComponents = getComponents(format.substring(0, format.indexOf("%message%")))
        val suffixComponents = getComponents(format.substring(format.indexOf("%message%")))

        prefixComponents.forEach {
            it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(getComponents(hover)))
            it.clickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.com/channels/${message.getGuild().id.value}/${message.channelId.value}/${message.id.value}")
        }

        val broadcast = (components as MutableList<BaseComponent>).map { it }.toTypedArray()

        plugin.server.spigot().broadcast(*broadcast)
    }

    private fun getComponents(string: String) : Array<BaseComponent> {
        var message = string

        // For some reason the reset code will not reset the message, but use the latest hex code.
        // So here we'll replace it with '&f', to get the expected effect.
        message = message.replace("&r", "&f")

        val componentBuilder = ComponentBuilder()

        val pattern = Pattern.compile("&#[a-fA-F0-9]{6}")
        var matcher = pattern.matcher(message)
        var color = ""

        // Append a new component every time a hex code is found.
        while (matcher.find()) {
            componentBuilder.append(message.substring(0, matcher.start()))
            if (color.isNotEmpty()) componentBuilder.currentComponent.color = ChatColor.of(color)

            color = message.substring(matcher.start() + 1, matcher.end())
            message = message.substring(matcher.end())
            matcher = pattern.matcher(message)
        }

        // Append the remainder of the message.
        componentBuilder.append(message)
        if (color.isNotEmpty()) componentBuilder.currentComponent.color = ChatColor.of(color)

        return componentBuilder.create()
    }

    private suspend fun getComponentss(string: String, content: String, guild: Guild): Array<BaseComponent> {
        var message = string
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