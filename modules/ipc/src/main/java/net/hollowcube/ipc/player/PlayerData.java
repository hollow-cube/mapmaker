package net.hollowcube.ipc.player;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.util.JsonValueAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record PlayerData(
    UUID id,
    String username,
    DisplayName displayName,
    @JsonAdapter(JsonValueAdapter.class) JsonObject settings,
    long playtime,
    long cubits,
    @Nullable Instant hypercubeUntil,
    long permissions,
    int mapSlots,
    MapSize maxMapSize,
    int mapBuilders,
    long coins
) {

    public boolean isHypercube() {
        return hypercubeUntil != null && hypercubeUntil.isAfter(Instant.now());
    }

    public boolean has(long flags) {
        return (permissions & flags) == flags;
    }

    public PlayerData withCoins(long coins) {
        return new PlayerData(
            id,
            username,
            displayName,
            settings,
            playtime,
            cubits,
            hypercubeUntil,
            permissions,
            mapSlots,
            maxMapSize,
            mapBuilders,
            coins
        );
    }

    public PlayerData withCubits(long cubits) {
        return new PlayerData(
            id,
            username,
            displayName,
            settings,
            playtime,
            cubits,
            hypercubeUntil,
            permissions,
            mapSlots,
            maxMapSize,
            mapBuilders,
            coins
        );
    }

    public PlayerData withMapLimits(int mapSlots, MapSize maxMapSize, int mapBuilders) {
        return new PlayerData(
            id,
            username,
            displayName,
            settings,
            playtime,
            cubits,
            hypercubeUntil,
            permissions,
            mapSlots,
            maxMapSize,
            mapBuilders,
            coins
        );
    }
}
