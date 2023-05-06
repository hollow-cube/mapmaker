package net.hollowcube.mapmaker.service;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * Implements the interface for the server to get/set information about players.
 * <p>
 * Eventually this would roughly represent the base requirements for an external player μService.
 */
public interface PlayerService {

    /**
     * Returns the current display name of the player. The display name includes any prefixes and suffixes,
     * but will never include extra data such as a chat channel suffix or chat status color.
     *
     * @return The display name of the player, or {@link #ERR_NOT_FOUND} if the player is not online.
     */
    @Blocking
    @NotNull Component getDisplayName(@NotNull String playerId);

    class NotFoundError extends RuntimeException {
    }

}
