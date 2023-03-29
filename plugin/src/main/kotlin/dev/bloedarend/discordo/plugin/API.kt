package dev.bloedarend.discordo.plugin

import dev.bloedarend.discordo.plugin.utils.Images
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import io.ktor.client.request.forms.*
import io.ktor.util.cio.*
import io.ktor.util.cio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import net.md_5.bungee.api.chat.TextComponent
import java.io.InputStream

open class API {

    companion object {
        private lateinit var util: Images
        private lateinit var client: Kord
        private lateinit var guildId: String
        private lateinit var channelId: String

        internal fun setUtil(util: Images) {
            this.util = util
        }

        internal fun setClient(client: Kord) {
            this.client = client
        }

        internal fun setGuildId(guildId: String) {
            this.guildId = guildId
        }

        internal fun setChannelid(channelId: String) {
            this.channelId = channelId
        }

        private suspend fun createMessage(inputStream: InputStream, channelId: String = this.channelId) {
            if (!::client.isInitialized || !::guildId.isInitialized || !::channelId.isInitialized) return

            val guild = client.getGuildOrNull(Snowflake(guildId)) ?: throw java.lang.Exception("Guild with guildId '${guildId}' could not be found.")
            val channel = guild.getChannelOfOrNull<TextChannel>(Snowflake(channelId)) ?: throw java.lang.Exception("Channel with channelId '${channelId}' could not be found.")

            channel.createMessage {
                val provider = ChannelProvider {
                    inputStream.toByteReadChannel()
                }

                addFile("discordo.png", provider)
            }
        }

        /**
         * Sends given input drawn on an image to Discord.
         *
         * @param string The string that is drawn onto the image.
         * @param channelId The id of the channel to which the image is sent. Leave empty to use the channel defined in the main config file.
         */
        @JvmStatic
        @OptIn(DelicateCoroutinesApi::class)
        fun sendImage(string: String, channelId: String = this.channelId) = GlobalScope.future {
            createMessage(util.getInputStream(string), channelId)
        }

        /**
         * Sends given input drawn on an image to Discord.
         *
         * @param textComponent The text component that is drawn onto the image.
         * @param channelId The id of the channel to which the image is sent. Leave empty to use the channel defined in the main config file.
         */
        @JvmStatic
        @OptIn(DelicateCoroutinesApi::class)
        fun sendImage(textComponent: TextComponent, channelId: String = this.channelId) = GlobalScope.future {
            createMessage(util.getInputStream(textComponent), channelId)
        }
    }

}