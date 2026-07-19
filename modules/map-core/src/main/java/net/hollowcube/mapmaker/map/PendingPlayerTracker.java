package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/// Tracks players which own a world lifecycle lease before they are visible in the world itself.
/// Thread safety is provided by the owner so admission checks and world state changes can use the same lock.
final class PendingPlayerTracker<W, P> {
    private final Map<P, W> worldsByPlayer = new IdentityHashMap<>();
    private final Map<W, Set<P>> playersByWorld = new IdentityHashMap<>();

    boolean reserve(@NotNull W world, @NotNull P player) {
        var previousWorld = worldsByPlayer.get(player);
        if (previousWorld != null) return previousWorld == world;

        worldsByPlayer.put(player, world);
        playersByWorld.computeIfAbsent(world, _ -> Collections.newSetFromMap(new IdentityHashMap<>()))
            .add(player);
        return true;
    }

    boolean hasPendingPlayers(@NotNull W world) {
        var players = playersByWorld.get(world);
        return players != null && !players.isEmpty();
    }

    /// Releases a player's lease.
    ///
    /// @return the world if this was its last pending player, otherwise null
    @Nullable W release(@NotNull P player) {
        var world = worldsByPlayer.remove(player);
        if (world == null) return null;

        var players = playersByWorld.get(world);
        if (players == null || !players.remove(player) || !players.isEmpty()) return null;

        playersByWorld.remove(world);
        return world;
    }
}
