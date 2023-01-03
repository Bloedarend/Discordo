package dev.bloedarend.discordo.kord.events

import com.vdurmont.emoji.EmojiParser
import dev.bloedarend.discordo.plugin.utils.Configs
import dev.bloedarend.discordo.plugin.utils.Messages
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.Guild
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

class MessageCreate(configs: Configs, private val messages: Messages, private val plugin: Plugin) {

    private val config = configs.getConfig("config")

    private val allowMessages = config?.getBoolean("minecraft.allow-messages") ?: true
    private val allowColorCodes = config?.getBoolean("minecraft.allow-color-codes") ?: false
    private val displayMentionsNicely = config?.getBoolean("minecraft.mentions.display-nicely") ?: true
    private val highlightMentions = config?.getBoolean("minecraft.mentions.highlight") ?: true
    private val displayEmotesNicely = config?.getBoolean("minecraft.emotes.display-nicely") ?: true
    private val removeEmotes = config?.getBoolean("minecraft.emotes.remove") ?: false

    suspend fun onMessageCreate(event: MessageCreateEvent) {
        val message = event.message

        if (!allowMessages) return
        if (message.author?.isBot == true) return
        if (removeEmotes && EmojiParser.removeAllEmojis(message.content).isEmpty()) return

        val member = message.getAuthorAsMember() ?: return
        val guild = member.guild.asGuild()
        val roles = member.roles.toList().sortedDescending()

        // Get the first colored role of the user
        val role = roles.firstOrNull {
            it.color != Color(0x99AAB5)
        }

        val roleColor = role?.color ?: Color(170, 170, 170)
        val roleName = role?.name ?: if (roles.isEmpty()) "" else roles.first().name

        val memberRoles = roles.map {
            messages.getMessage("minecraft.member-roles.format", null,
                Pair("%current_role_name%", it.name),
                Pair("%current_role_color%", String.format("&#%02x%02x%02x", it.color.red, it.color.green, it.color.blue))
            )
        }

        val placeholders = arrayOf(
            Pair("%member_name%", member.username),
            Pair("%member_displayname%", member.displayName),
            Pair("%member_tag%", member.tag),
            Pair("%member_roles%", memberRoles.toList().joinToString(messages.getMessage("minecraft.member-roles.separator"))),
            Pair("%role_name%", roleName),
            Pair("%role_color%", String.format("&#%02x%02x%02x", roleColor.red, roleColor.green, roleColor.blue))
        )

        val components = getComponents("minecraft.format", message.content, guild, *placeholders)
        val hover = getComponents("minecraft.hover", message.content, guild, *placeholders)
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

        if (displayMentionsNicely) {
            val memberPattern = Pattern.compile("<@[0-9]{17,20}>")
            var memberMatcher = memberPattern.matcher(messageContent)
            while(memberMatcher.find()) {
                val member = guild.members.firstOrNull { it.id.value.toString() == messageContent.substring(memberMatcher.start() + 2, memberMatcher.end() - 1) }
                if (member != null) {
                    messageContent = messageContent.replace(messageContent.substring(memberMatcher.start(), memberMatcher.end()), "@${member.displayName}")
                    memberMatcher = memberPattern.matcher(messageContent)
                }
            }

            val rolePattern = Pattern.compile("<@&[0-9]{17,20}>")
            var roleMatcher = rolePattern.matcher(messageContent)
            while (roleMatcher.find()) {
                val role = guild.roles.firstOrNull { it.id.value.toString() == messageContent.substring(roleMatcher.start() + 3, roleMatcher.end() -1) }
                if (role != null) {
                    messageContent = messageContent.replace(messageContent.substring(roleMatcher.start(), roleMatcher.end()), "@${role.name}")
                    roleMatcher = rolePattern.matcher(messageContent)
                }
            }

            val channelPattern = Pattern.compile("<#[0-9]{17,20}>")
            var channelMatcher = channelPattern.matcher(messageContent)
            while (channelMatcher.find()) {
                val channel = guild.channels.firstOrNull { it.id.value.toString() == messageContent.substring(channelMatcher.start() + 2, channelMatcher.end() - 1) }
                if (channel != null) {
                    val icon = if (channel.type == ChannelType.GuildVoice || channel.type == ChannelType.GuildStageVoice) "♪" else "#"

                    messageContent = messageContent.replace(messageContent.substring(channelMatcher.start(), channelMatcher.end()), "${icon}${channel.name}")
                    channelMatcher = channelPattern.matcher(messageContent)
                }
            }

            val emotePattern = Pattern.compile("<a?:[a-zA-Z_0-9]+:[0-9]{17,20}>")
            var emoteMatcher = emotePattern.matcher(messageContent)
            while (emoteMatcher.find()) {
                if (removeEmotes) messageContent = messageContent.replace(messageContent.substring(emoteMatcher.start(), emoteMatcher.end()), "")
                else if (displayEmotesNicely) {
                    val emote = messageContent.substring(emoteMatcher.start(), emoteMatcher.end())
                    messageContent = messageContent.replace(messageContent.substring(emoteMatcher.start(), emoteMatcher.end()), emote.substring(emote.indexOf(':'), emote.lastIndexOf(':') + 1))
                }

                emoteMatcher = emotePattern.matcher(messageContent)
            }
        }

        if (removeEmotes) messageContent = EmojiParser.removeAllEmojis(messageContent)
        else if (displayEmotesNicely) messageContent = EmojiParser.parseToAliases(messageContent)

        message = if (allowColorCodes) message.replace("%message%", org.bukkit.ChatColor.translateAlternateColorCodes('&', messageContent))
        else {
            // Because we will be making a component, hex codes won't work anyway, so this is a nice way to get rid of them.
            messages.formatHexMessage(content)
            message.replace("%message%", messageContent)
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
}