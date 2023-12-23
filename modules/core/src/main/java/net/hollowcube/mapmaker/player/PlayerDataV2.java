package net.hollowcube.mapmaker.player;

import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerDataV2 {
    public static final Tag<PlayerDataV2> TAG = Tag.Transient("mapmaker:player_data");

    public static @NotNull PlayerDataV2 fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    public static final int MAX_MAP_SLOTS = 5;

    private transient PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    private String id;
    private String username;
    @SerializedName("display_name_v2")
    private DisplayName displayNameV2 = new DisplayName(List.of());
    private PlayerSettings settings = new PlayerSettings();

    private transient long sessionStart = System.currentTimeMillis(); //todo this should be set by the session service
    private long playtime; // in milliseconds since last save (when session was created)

    private int coins = 0;
    private int cubits = 0;

    public PlayerDataV2(String id, String username, @NotNull TextComponent displayName) {
        this.id = id;
        this.username = username;
    }

    public PlayerDataV2(String id, String username, DisplayName displayName, PlayerSettings settings, long playtime, int coins, int cubits) {
        this.id = id;
        this.username = username;
        this.displayNameV2 = displayName;
        this.settings = settings;
        this.playtime = playtime;
        this.coins = coins;
        this.cubits = cubits;
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
        return displayNameV2.asComponent();
    }

    public @NotNull DisplayName displayName2() {
        return displayNameV2;
    }

    public @NotNull PlayerSettings settings() {
        return settings;
    }

    public long storedPlaytime() {
        return playtime;
    }

    public long sessionPlaytime() {
        return System.currentTimeMillis() - sessionStart;
    }

    public long totalPlaytime() {
        return storedPlaytime() + sessionPlaytime();
    }

    public int coins() {
        return coins;
    }

    public int cubits() {
        return cubits;
    }

}
