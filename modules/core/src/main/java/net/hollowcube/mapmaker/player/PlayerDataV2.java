package net.hollowcube.mapmaker.player;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

public class PlayerDataV2 {
    public static final Tag<PlayerDataV2> TAG = Tag.Transient("mapmaker:player_data");
    public static @NotNull PlayerDataV2 fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    public static final int MAX_MAP_SLOTS = 5;

    private transient PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    private String id;
    private String username;
    private Component displayName;
    private PlayerSettings settings = new PlayerSettings();
//
//    private int unlockedMapSlots = 2;
//    private String[] mapSlots = new String[MAX_MAP_SLOTS];
//    private @Nullable String lastPlayedMap = null;
//    private @Nullable String lastEditedMap = null;

    private String tfState = null; // base64 bytes

    public PlayerDataV2() {
    }

    public PlayerDataV2(@NotNull String id, @NotNull String username, @NotNull Component displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public @NotNull PlayerDataUpdateRequest getUpdateRequest() {
        var updates = this.updates;
        this.updates = new PlayerDataUpdateRequest();
        return updates;
    }

    public @NotNull String id() {
        return id;
    }
    public @NotNull String username() {
        return username;
    }
    public @NotNull Component displayName() {
        return displayName;
    }
    public @NotNull PlayerSettings settings() {
        return settings;
    }

    public byte @Nullable [] getTfState() {
        if (tfState == null) return null;
        return Base64.getDecoder().decode(tfState);
    }

    public void setTfState(byte @Nullable [] tfState) {
        if (tfState == null) this.tfState = null;
        else this.tfState = Base64.getEncoder().encodeToString(tfState);
        updates.setTfState(this.tfState);
    }
}
