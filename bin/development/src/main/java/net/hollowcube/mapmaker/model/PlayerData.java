package net.hollowcube.mapmaker.model;

import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * MapMaker data for a single player.
 */
public class PlayerData {
    // The player's network id, which can be different from their UUID
    public static final Tag<String> PLAYER_ID = Tag.String("mapmaker:player_id");

    private final String id;

    public PlayerData(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }
}
