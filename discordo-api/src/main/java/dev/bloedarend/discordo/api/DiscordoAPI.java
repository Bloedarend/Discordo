package dev.bloedarend.discordo.api;

import kotlin.Unit;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.concurrent.CompletableFuture;

public interface DiscordoAPI {

    /**
     * Send an image to the channel defined in the discordo config.yml
     *
     * @param string The string that will be drawn onto the image.
     */
    CompletableFuture<Unit> sendImage(String string);

    /**
     * Send an image to a custom channel.
     *
     * @param string The string that will be drawn onto the image.
     * @param channelId The id of the channel to send the image to.
     */
    CompletableFuture<Unit> sendImage(String string, String channelId);

    /**
     * Send an image to the channel defined in the discordo config.yml
     *
     * @param textComponent The text component that will be drawn onto the image.
     */
    CompletableFuture<Unit> sendImage(TextComponent textComponent);

    /**
     * Send an image to a custom channel.
     *
     * @param textComponent The text component that will be drawn onto the image.
     * @param channelId The id of the channel to send the image to.
     */
    CompletableFuture<Unit> sendImage(TextComponent textComponent, String channelId);

}
