package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.util.gson.UnsignedLongAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RuntimeGson
public class PlayerData {
    public static final Tag<PlayerData> TAG = Tag.Transient("mapmaker:player_data");

    private static final long[] XP_FOR_LEVELS = new long[51];

    public static @NotNull PlayerData fromPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private transient PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    private String id;
    private String username;
    private DisplayName displayNameV2 = new DisplayName(List.of());
    private DisplayName displayName; // v4
    private JsonObject settings = new JsonObject();

    @Expose(serialize = false, deserialize = false)
    public final long sessionStart = System.currentTimeMillis(); //todo this should be set by the session service
    private long playtime; // in milliseconds since last save (when session was created)
    private long experience;

    private int coins = 0;
    private int cubits = 0;
    private @Nullable Instant hypercubeUntil;

    @MagicConstant(flagsFromClass = Permission.class)
    @JsonAdapter(UnsignedLongAdapter.class)
    private long permissions;
    private int mapSlots;
    private MapSize tempMaxMapSize;
    private int mapBuilders;

    public PlayerData() {
    }

    public PlayerData(String id, String username, @NotNull TextComponent displayName) {
        this.id = id;
        this.username = username;
    }

    public PlayerData(String id, String username, DisplayName displayName, JsonObject settings, long playtime, int coins, int cubits) {
        this.id = id;
        this.username = username;
        this.displayNameV2 = displayName;
        this.settings = settings;
        this.playtime = playtime;
        this.coins = coins;
        this.cubits = cubits;
    }

    @VisibleForTesting
    public PlayerData(@NotNull Player player) {
        this(player.getUuid().toString(),
            player.getUsername(),
            new DisplayName(List.of(new DisplayName.Part("username", player.getUsername(), null))),
            new JsonObject(), 0, 0, 0
        );
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
            ExceptionReporter.reportException(e); // Dont associate with the user, we don't know if they are the initiator
            return false;
        }
    }

    public boolean writeUpdatesUpstream(@NotNull PlayerClient players) {
        //todo need to add a lock here
        var settingChanges = updates.settings();
        if (settingChanges == null) return true;
        try {
            players.updatePlayerSettings(id, settingChanges);
            updates = new PlayerDataUpdateRequest();
            return true;
        } catch (Exception e) {
            ExceptionReporter.reportException(e); // Dont associate with the user, we don't know if they are the initiator
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
        return displayName2().asComponent();
    }

    public @NotNull DisplayName displayName2() {
        return displayName != null ? displayName : displayNameV2;
    }

    public <T> @NotNull T getSetting(@NotNull PlayerSetting<T> setting) {
        return setting.read(settings());
    }

    public <T> void setSetting(@NotNull PlayerSetting<T> setting, @NotNull T value) {
        var raw = setting.write(value);
        settings().add(setting.key(), raw);
        updates.updateSetting(setting.key(), raw);
    }

    public void resetSetting(@NotNull PlayerSetting<?> setting) {
        settings().remove(setting.key());
        updates.updateSetting(setting.key(), JsonNull.INSTANCE);
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

    public long experience() {
        return experience;
    }

    public int level() {
        for (int i = 0; i < XP_FOR_LEVELS.length; i++) {
            if (experience < XP_FOR_LEVELS[i]) return i - 1;
        }
        return XP_FOR_LEVELS.length - 1;
    }

    public float levelProgress() {
        var level = level();
        if (level == 50) return 1;
        var prev = XP_FOR_LEVELS[level];
        var next = XP_FOR_LEVELS[level + 1];
        return (float) (experience - prev) / (next - prev);
    }

    public void setExperience(long experience) {
        this.experience = experience;
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

    @Deprecated
    public void updateFromMapUpgrade(int mapSlots, MapSize maxMapSize, int mapBuilders) {
        this.mapSlots += mapSlots;
        this.tempMaxMapSize = MapSize.values()[Math.max(this.tempMaxMapSize.id(), maxMapSize.id())];
        if (mapBuilders > 0) {
            this.mapBuilders = Math.max(this.mapBuilders, 1 + mapBuilders); // 1 is default then set to mapBuilders
        }
    }

    public boolean isHypercube() {
        return hypercubeUntil != null && hypercubeUntil.isAfter(Instant.now());
    }

    public boolean has(@MagicConstant(flagsFromClass = Permission.class) long perms) {
        return (permissions & perms) == perms;
    }

    public int mapSlots() {
        return mapSlots;
    }

    public MapSize maxMapSize() {
        return tempMaxMapSize;
    }

    public int mapBuilders() {
        return mapBuilders;
    }

    public @Nullable String getCosmetic(@NotNull CosmeticType type) {
        var cosmetic = getSetting(type.setting());
        return cosmetic.isEmpty() ? null : cosmetic;
    }

    public void setCosmetic(@NotNull CosmeticType type, @Nullable Cosmetic cosmetic) {
        if (cosmetic != null && cosmetic.type() != type) throw new IllegalArgumentException("cosmetic type mismatch");
        setSetting(type.setting(), cosmetic == null ? "" : cosmetic.id());
    }

    static {
        double BASE_XP = 100;
        double GROWTH_FACTOR = 1.1;
        for (int i = 0; i < XP_FOR_LEVELS.length; i++) {
            XP_FOR_LEVELS[i] = (long) (i == 0 ? 0 : XP_FOR_LEVELS[i - 1] + ((BASE_XP * Math.pow(GROWTH_FACTOR, i - 1)) + 100));
        }
    }
}
