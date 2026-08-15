package net.hollowcube.compat.api.discord;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DiscordRichPresenceProvider {

    /**
     * Sets the rich presence for a player.
     * Format:
     * 0: "<activity> <name> on Hollow Cube"
     * 1: "<details?>" If details is null or empty, this line will default to whaterver the provide wants, on lunar this is the mc version, on feather nothing.
     */
    void setRichPresence(
        @NotNull Player player,
        @NotNull String activity, @NotNull String name,
        @Nullable String details
    );

    void clearRichPresence(@NotNull Player player);

    boolean isRichPresenceSupportedFor(@NotNull Player player);
}
