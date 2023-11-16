package net.hollowcube.terraform.storage;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Storage API for Terraform.
 *
 * <p>Implementations may block during calls to this interface, the caller is
 * responsible for ensuring safe callsites.</p>
 */
@Blocking
public interface TerraformStorage {

    /**
     * Loads the player session data for the given player and player ID if it exists.
     *
     * @param player   The player for which to load the session.
     * @param playerId The ID of the player (not necessarily the player UUID).
     * @return The player session data, or null if there is no saved data.
     */
    byte @Nullable [] loadPlayerSession(@NotNull String playerId);

    /**
     * Saves the player session.
     *
     * @param session The player session to be saved.
     */
    void savePlayerSession(@NotNull String playerId, byte @NotNull [] session);

    /**
     * Loads a local session data for a given player, player ID, and instance ID, if it exists.
     *
     * @param player     The player for which to load the session (must be in an {@link Instance})
     * @param playerId   The ID of the player (not necessarily the player UUID).
     * @param instanceId The ID of the instance (not necessarily the instance UUID).
     * @return The loaded local session, or null if there is no saved data.
     */
    byte @Nullable [] loadLocalSession(@NotNull String playerId, @NotNull String instanceId);

    /**
     * Saves the local session.
     *
     * @param session The local session to be saved.
     */
    void saveLocalSession(@NotNull String playerId, @NotNull String instanceId, byte @NotNull [] session);

}
