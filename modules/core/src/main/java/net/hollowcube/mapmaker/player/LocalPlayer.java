package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// The shared [PlayerData] snapshot plus what only this server knows about the session:
/// how long it has been running and which settings changed and still have to be written back.
public final class LocalPlayer {
    public static final Tag<LocalPlayer> TAG = Tag.Transient("mapmaker:player_data");

    public static @NotNull LocalPlayer localPlayer(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private final AtomicReference<PlayerData> info;
    private PlayerDataUpdateRequest updates = new PlayerDataUpdateRequest();

    public final long sessionStart = System.currentTimeMillis(); //todo this should be set by the session service

    public LocalPlayer(@NotNull PlayerData info) {
        this.info = new AtomicReference<>(info);
    }

    @TestOnly
    public LocalPlayer(@NotNull Player player) {
        this(player.getUuid().toString(), player.getUsername());
    }

    @TestOnly
    public LocalPlayer(@NotNull String id, @NotNull String username) {
        this(new PlayerData(id, username, DisplayName.of(username), new JsonObject(),
            0, 0, null, 0, 0, MapSize.NORMAL, 0, 0));
    }

    public @NotNull PlayerData info() {
        return info.get();
    }

    public boolean writeUpdatesUpstream(@NotNull PlayerService playerService) {
        //todo need to add a lock here
        if (!updates.hasChanges()) return true;
        try {
            playerService.updatePlayerData(id(), updates);
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
            players.updatePlayerSettings(id(), settingChanges);
            updates = new PlayerDataUpdateRequest();
            return true;
        } catch (Exception e) {
            ExceptionReporter.reportException(e); // Dont associate with the user, we don't know if they are the initiator
            return false;
        }
    }

    public @NotNull String id() {
        return info.get().id();
    }

    public @NotNull String username() {
        return info.get().username();
    }

    public @NotNull Component displayName() {
        return info.get().displayName().render();
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
        return info.get().settings();
    }

    public long storedPlaytime() {
        return info.get().playtime();
    }

    public long sessionPlaytime() {
        return System.currentTimeMillis() - sessionStart;
    }

    public long totalPlaytime() {
        return storedPlaytime() + sessionPlaytime();
    }

    public int coins() {
        return Math.toIntExact(info.get().coins());
    }

    public void setCoins(int coins) {
        info.updateAndGet(value -> value.withCoins(coins));
    }

    public int cubits() {
        return Math.toIntExact(info.get().cubits());
    }

    public void setCubits(int cubits) {
        info.updateAndGet(value -> value.withCubits(cubits));
    }

    @Deprecated
    public void updateFromMapUpgrade(int mapSlots, MapSize maxMapSize, int mapBuilders) {
        info.updateAndGet(value -> {
            var size = value.maxMapSize() == MapSize.UNLIMITED || value.maxMapSize().id() >= maxMapSize.id()
                ? value.maxMapSize() : maxMapSize;
            var builders = mapBuilders > 0
                ? Math.max(value.mapBuilders(), 1 + mapBuilders) // 1 is default then set to mapBuilders
                : value.mapBuilders();
            return value.withMapLimits(value.mapSlots() + mapSlots, size, builders);
        });
    }

    public boolean isHypercube() {
        return info.get().isHypercube();
    }

    public boolean has(@MagicConstant(flagsFromClass = Permission.class) long perms) {
        return info.get().has(perms);
    }

    public int mapSlots() {
        return info.get().mapSlots();
    }

    public MapSize maxMapSize() {
        var size = info.get().maxMapSize();
        if (isHypercube() && size != MapSize.UNLIMITED && size.id() < MapSize.MASSIVE.id()) return MapSize.MASSIVE;
        return size;
    }

    public int mapBuilders() {
        return info.get().mapBuilders();
    }

    public @Nullable String getCosmetic(@NotNull CosmeticType type) {
        var cosmetic = getSetting(type.setting());
        return cosmetic.isEmpty() ? null : cosmetic;
    }

    public void setCosmetic(@NotNull CosmeticType type, @Nullable Cosmetic cosmetic) {
        if (cosmetic != null && cosmetic.type() != type) throw new IllegalArgumentException("cosmetic type mismatch");
        setSetting(type.setting(), cosmetic == null ? "" : cosmetic.id());
    }
}
