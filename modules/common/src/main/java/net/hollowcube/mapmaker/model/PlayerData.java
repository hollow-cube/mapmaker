package net.hollowcube.mapmaker.model;

import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * MapMaker data for a single player.
 */
public class PlayerData {
    public static final Tag<String> PLAYER_ID = Tag.String("mapmaker:player_id");

    public static final Tag<PlayerData> DATA = TagUtil.noop("mapmaker:player_data");

    // The player's network id, which can be different from their UUID
    private final String id;

    public PlayerData(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "id='" + id + '\'' +
                '}';
    }
}
