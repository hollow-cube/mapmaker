package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PlayerDataV2 {
    private static final Logger logger = LoggerFactory.getLogger(PlayerDataV2.class);

    public static final Tag<PlayerDataV2> TAG = Tag.Transient("mapmaker:player_data");

    public static @NotNull PlayerDataV2 fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    public static final int MAX_MAP_SLOTS = 5;

    private transient PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    private String id;
    private String username;
    private DisplayName displayNameV2 = new DisplayName(List.of());
    private JsonObject settings = new JsonObject();

    @Expose(serialize = false, deserialize = false)
    public final long sessionStart = System.currentTimeMillis(); //todo this should be set by the session service
    private long playtime; // in milliseconds since last save (when session was created)

    private int coins = 0;
    private int cubits = 0;

    private Set<String> unlockedCosmetics = new HashSet<>();

    public PlayerDataV2() {
    }

    public PlayerDataV2(String id, String username, @NotNull TextComponent displayName) {
        this.id = id;
        this.username = username;
    }

    public PlayerDataV2(String id, String username, DisplayName displayName, JsonObject settings, long playtime, int coins, int cubits) {
        this.id = id;
        this.username = username;
        this.displayNameV2 = displayName;
        this.settings = settings;
        this.playtime = playtime;
        this.coins = coins;
        this.cubits = cubits;
    }

    /**
     * Writes all updates to the database.
     *
     * @param playerService
     * @return true if the update was successful, false if it failed (an error was already logged)
     */
    public boolean writeUpdatesUpstream(@NotNull PlayerService playerService) {
        //todo need to add a lock here
        if (!updates.hasChanges()) return true;
        try {
            playerService.updatePlayerData(id, updates);
            updates = new PlayerDataUpdateRequest();
            return true;
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            return false;
        }
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

    public <T> @NotNull T getSetting(@NotNull PlayerSetting<T> setting) {
        return setting.read(settings());
    }

    public <T> void setSetting(@NotNull PlayerSetting<T> setting, @NotNull T value) {
        var raw = setting.write(value);
        settings().add(setting.key(), raw);
        updates.updateSetting(setting.key(), raw);
    }

    public @NotNull Collection<Map.Entry<String, JsonElement>> settingsRawValues() {
        return settings().entrySet();
    }

    private @NotNull JsonObject settings() {
        if (settings == null) settings = new JsonObject();
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

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int cubits() {
        return cubits;
    }

    public void setCubits(int cubits) {
        this.cubits = cubits;
    }

    public @Nullable String getCosmetic(@NotNull CosmeticType type) {
        var cosmetic = getSetting(type.setting());
        return cosmetic.isEmpty() ? null : cosmetic;
    }

    public void setCosmetic(@NotNull CosmeticType type, @Nullable Cosmetic cosmetic) {
        if (cosmetic != null && cosmetic.type() != type) throw new IllegalArgumentException("cosmetic type mismatch");
        setSetting(type.setting(), cosmetic == null ? "" : cosmetic.id());
    }

    public @NotNull Set<String> unlockedCosmetics() {
        return unlockedCosmetics == null ? Set.of() : unlockedCosmetics;
    }


}
