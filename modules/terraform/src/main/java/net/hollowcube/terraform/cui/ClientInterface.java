package net.hollowcube.terraform.cui;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Client interface represents the available client user interface actions for a player.
 * <p>
 * This interface should be the exclusive way to interact with a client from Terraform.
 */
public interface ClientInterface {

    /**
     * Sends a message to the client interface with the given key and args. This is a thin facade over some translation
     * system behind it.
     * <p>
     * The args can be any object, which will be converted to a string unless it is a {@link Component}, which will
     * be used in place. Stringified args will always have an unset color and styling.
     *
     * @param key Message key associated with the message
     * @param args Arguments to the message key
     */
    void sendMessage(@NotNull String key, @NotNull Object... args);

    @NotNull ClientRenderer renderer();
}
