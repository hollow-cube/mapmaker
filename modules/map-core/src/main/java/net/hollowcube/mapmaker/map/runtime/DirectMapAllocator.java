package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

final class DirectMapAllocator implements MapAllocator {
    private static final Logger logger = LoggerFactory.getLogger(DirectMapAllocator.class);

    public static final Component CLOSED_MESSAGE = Component.translatable("map.closed");

    private final MapServer server;

    DirectMapAllocator(@NotNull MapServer server) {
        this.server = server;
    }

    @Override
    public <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull MapWorld.Constructor<T> ctor) {
        try {
            var world = ctor.create(server, map);
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

        // Unload the world
        world.close(reason);
    }

    @Override
    public @NotNull <T extends AbstractMapWorld> Future<@Nullable T> create(@NotNull MapData map, @NotNull MapWorld.Constructor<T> ctor) {
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

    @Override
    public void showDebugInfo(@NotNull Audience audience) {
        audience.sendMessage(Component.text("Direct allocator does not track maps."));
    }
}
