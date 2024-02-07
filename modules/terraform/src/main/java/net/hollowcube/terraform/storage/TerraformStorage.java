package net.hollowcube.terraform.storage;

import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicHeader;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Storage API for Terraform.
 *
 * <p>Implementations may block during calls to this interface, the caller is
 * responsible for ensuring safe callsites.</p>
 */
@Blocking
public interface TerraformStorage {

    // Sessions

    /**
     * Loads the player session data for the given player and player ID if it exists.
     *
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

    // Schematics

    @NotNull List<@NotNull SchematicHeader> listSchematics(@NotNull String playerId);

    @Nullable Schematic loadSchematicData(@NotNull String playerId, @NotNull String name);

    enum SchematicCreateResult {
        //this is lazy i guess
        SUCCESS,
        DUPLICATE_ENTRY, // Schematic with the same (normalized) name exists, never returned if overwrite is true
        SIZE_LIMIT_EXCEEDED, // Schematic is too large
        ENTRY_LIMIT_EXCEEDED, // Player has too many schematics
    }

    @NotNull SchematicCreateResult createSchematic(@NotNull String playerId, @NotNull String name, @NotNull Schematic schematic, boolean overwrite);

    enum SchematicDeleteResult {
        SUCCESS,
        NOT_FOUND,
    }

    @NotNull SchematicDeleteResult deleteSchematic(@NotNull String playerId, @NotNull String name);
}
