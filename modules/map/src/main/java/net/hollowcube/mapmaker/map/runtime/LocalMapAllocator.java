package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.metrics.MapInstanceCreatedEvent;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Implements local tracking of map allocations.
 *
 * <p>Currently only allows a single map of the given type to be created, and will lock to prevent the same map
 * from being created twice.</p>
 *
 * <p>(As of writing) another allocator will be necessary to report allocations to the session service
 * for tracking and player allocation.</p>
 */
public class LocalMapAllocator implements MapAllocator {
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Logger logger = LoggerFactory.getLogger(LocalMapAllocator.class);

    private final MetricWriter metrics;
    private final MapAllocator direct;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<MapKey, Future<AbstractMapWorld>> maps = new HashMap<>();

    private boolean isClosing = false;

    private record MapKey(@NotNull String mapId, @NotNull Class<?> worldType) {
    }

    public LocalMapAllocator(@NotNull MapServer server) {
        this.metrics = server.metrics();
        this.direct = MapAllocator.direct(server);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T extends AbstractMapWorld> Future<@Nullable T> create(@NotNull MapData map, @NotNull Class<T> worldType) {
        return VIRTUAL_EXECUTOR.submit(() -> {
            Future<T> future;

            // Small note about locking here.
            // We only lock around specifically the `maps` get and put operations and NOT
            // the actual allocation of the world which is submitted in another thread, and then
            // we suspend waiting for it after the lock is released.

            lock.lock();
            try {
                var key = new MapKey(map.id(), worldType);

                // Return existing world if present.
                future = (Future<T>) maps.get(key);
                if (future == null) {
                    // No existing world, create a new one and keep track of it
                    future = VIRTUAL_EXECUTOR.submit(() -> allocateTracked(map, worldType));
                    maps.put(key, (Future<AbstractMapWorld>) future);
                }
            } finally {
                lock.unlock();
            }

            return future.get();
        });
    }

    @Override
    public @NotNull Future<Boolean> destroy(@NotNull String worldId, @NotNull Component reason) {
        Callable<Boolean> task = () -> {
            for (var map : List.copyOf(maps.entrySet())) {
                var key = map.getKey();
                var world = FutureUtil.getUnchecked(map.getValue());
                if (!world.worldId().equals(worldId)) continue;

                lock.lock();
                try {
                    maps.remove(key);
                } finally {
                    lock.unlock();
                }
                direct.free(world, reason);
                return true;
            }

            return false;
        };
        return isClosing ? FutureUtil.callNow(task) : VIRTUAL_EXECUTOR.submit(task);
    }

    @Override
    public @NotNull Future<Integer> destroyAll(@NotNull String mapId, @NotNull Component reason) {
        var futures = new ArrayList<Future<?>>();

        lock.lock();
        try {
            for (var map : List.copyOf(maps.entrySet())) {
                var key = map.getKey();
                if (!key.mapId().equals(mapId)) continue;

                maps.remove(key);
                futures.add(VIRTUAL_EXECUTOR.submit(() -> direct.free(FutureUtil.getUnchecked(map.getValue()), reason)));
            }
        } finally {
            lock.unlock();
        }

        return VIRTUAL_EXECUTOR.submit(() -> {
            for (var future : futures)
                FutureUtil.getUnchecked(future);
            return futures.size();
        });
    }

    public void forEachWorld(@NotNull Consumer<MapWorld> func) {
        lock.lock();
        try {
            for (var map : List.copyOf(maps.values())) {
                func.accept(FutureUtil.getUnchecked(map));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            isClosing = true;
            for (var future : List.copyOf(maps.values())) {
                var world = FutureUtil.getUnchecked(future);
                FutureUtil.getUnchecked(destroy(world.worldId(), Component.translatable("mapmaker.shutdown")));
            }
        } finally {
            lock.unlock();
        }
    }

    // Small wrapper around direct allocator to keep track of players in the instance
    private <T extends AbstractMapWorld> T allocateTracked(@NotNull MapData map, @NotNull Class<T> worldType) {
        metrics.write(new MapInstanceCreatedEvent(map.id(), worldType.getSimpleName()));
        var createdWorld = direct.allocateDirect(map, worldType);
        createdWorld.instance().eventNode().addListener(PlayerInstanceLeaveEvent.class, event -> {
            // Get the world from the instance because 1: the player is no longer in a world, and 2: we care about the root world (editing, not testing)
            var world = MapWorld.unsafeFromInstance(event.getInstance());
            if (world == null) return;

            // If the owner has left, destroy the map on its next tick.
            var playerData = PlayerDataV2.fromPlayer(event.getPlayer());
            if (playerData.id().equals(world.map().owner()) && !world.map().isPublished()) {
                world.instance().scheduleNextTick(ignored -> destroy(world.worldId(), Component.translatable("map.kicked")));
                return;
            }

            // Stop if there are still players in the instance
            if (event.getInstance().getPlayers().size() > 1) return;

            destroy(world.worldId(), Component.translatable("map.closed"));
        });
        return createdWorld;
    }

    // Direct allocator delegated calls.

    @Override
    public <T extends AbstractMapWorld> @NotNull T allocateDirect(@NotNull MapData map, @NotNull Class<T> worldType) {
        return direct.allocateDirect(map, worldType);
    }

    @Override
    public void free(@NotNull MapWorld world, @Nullable Component reason) {
        direct.free(world, reason);
    }

    @Override
    public void showDebugInfo(@NotNull Audience audience) {
        lock.lock();
        try {
            var builder = Component.text();
            builder.append(Component.text("Local allocator (" + this.maps.size() + " active maps)"));
            for (var entry : maps.entrySet()) {
                builder.appendNewline().append(Component.text("»"));
                var key = entry.getKey();
                var mapIdShort = key.mapId().substring(0, Math.min(8, key.mapId().length()));

                builder.append(ComponentUtil.createBasicCopy(mapIdShort, key.mapId()));
                builder.append(Component.text(" (" + key.worldType().getSimpleName() + ")"));

                if (entry.getValue().isDone()) {
                    var world = FutureUtil.getUnchecked(entry.getValue());
                    var shortWorldId = world.worldId().substring(0, Math.min(8, world.worldId().length()));
                    builder.append(Component.text(": ").append(ComponentUtil.createBasicCopy(shortWorldId, world.worldId())));
                    world.appendDebugInfo(builder);
                } else {
                    builder.append(Component.text(": (loading)"));
                }
            }
            audience.sendMessage(builder);
        } finally {
            lock.unlock();
        }
    }
}
