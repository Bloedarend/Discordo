package dev.bloedarend.discordo.api

import net.md_5.bungee.api.chat.TextComponent
import java.util.concurrent.CompletableFuture

interface IDiscordo {

    /**
     * Send an image to the channel defined in the discordo config.yml
     *
     * @param string The string that will be drawn onto the image.
     */
    fun sendImage(string: String): CompletableFuture<Unit>

    /**
     * Send an image to a custom channel.
     *
     * @param string The string that will be drawn onto the image.
     * @param channelId The id of the channel to send the image to.
     */
    fun sendImage(string: String, channelId: String): CompletableFuture<Unit>

    /**
     * Send an image to the channel defined in the discordo config.yml
     *
     * @param textComponent The text component that will be drawn onto the image.
     */
    fun sendImage(textComponent: TextComponent): CompletableFuture<Unit>

    /**
     * Send an image to a custom channel.
     *
     * @param textComponent The text component that will be drawn onto the image.
     * @param channelId The id of the channel to send the image to.
     */
    fun sendImage(textComponent: TextComponent, channelId: String): CompletableFuture<Unit>

}