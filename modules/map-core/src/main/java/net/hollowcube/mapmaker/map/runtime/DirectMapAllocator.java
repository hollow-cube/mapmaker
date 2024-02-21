package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

final class DirectMapAllocator implements MapAllocator {
    private static final Logger logger = LoggerFactory.getLogger(DirectMapAllocator.class);

    private static final Component CLOSED_MESSAGE = Component.translatable("map.closed");

    private final MapServer server;

    DirectMapAllocator(@NotNull MapServer server) {
        this.server = server;
    }

    @Override
    public <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType) {
        try {
            var world = server.createInstance(worldType, Map.of(MapData.class, map));
            world.load();
            return world;
        } catch (Exception e) {
            logger.error("failed to allocate world", e);
            throw new RuntimeException("failed to allocate world", e);
        }
    }

    @Override
    public void free(@NotNull MapWorld mapWorld, @Nullable Component reason) {
        reason = reason == null ? CLOSED_MESSAGE : reason;

        //todo There is a race here i think of someone joining before close. May need to lock the world for this.
        // Anyway LocalMapAllocator locks in relevant sections so this should never happen when the 'high level' api
        // is being used.
        var world = (AbstractMapWorld) mapWorld;

        // Send all players to the hub
        removePlayerSet(world.players(), reason);
        removePlayerSet(world.spectators(), reason);

        // Unload the world
        world.close();
    }

    @Override
    public @NotNull <T extends AbstractMapWorld> Future<@Nullable T> create(@NotNull MapData map, @NotNull Class<T> worldType) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull Future<Boolean> destroy(@NotNull String worldId, @NotNull Component reason) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull Future<Integer> destroyAll(@NotNull String mapId, @NotNull Component reason) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public void close() {
        // We have no worlds tracked, so cannot close any
    }

    private void removePlayerSet(@NotNull Collection<Player> players, @NotNull Component reason) {
        for (var player : Set.copyOf(players)) {
            try {
                player.sendMessage(reason);
                server.bridge().joinHub(player);
            } catch (Exception e) {
                logger.error("failed to move player to hub ({})", player.getUuid(), e);
                MinecraftServer.getExceptionManager().handleException(e);
                player.kick(CLOSED_MESSAGE);
            }
        }
    }
}
