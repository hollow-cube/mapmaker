package net.hollowcube.map.world;

import kotlin.Pair;
import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MapWorldManager {
    private static final System.Logger logger = System.getLogger(MapWorldManager.class.getName());
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<Pair<String, Boolean>, Future<InternalMapWorld>> activeMaps = new ConcurrentHashMap<>();
    private final MapServer server;

    public MapWorldManager(@NotNull MapServer server) {
        this.server = server;

        MinecraftServer.getGlobalEventHandler().addListener(PlayerInstanceLeaveEvent.class, event -> {
            // Get the world from the instance because 1: the player is no longer in a world, and 2: we care about the root world (editing, not testing)
            var world = MapWorld.unsafeFromInstance(event.getInstance());
            if (world == null) return;

            // Stop if there are still players in the instance
            if (event.getInstance().getPlayers().size() > 1) return;

            var removed = activeMaps.remove(new Pair<>(world.map().id(), (world.flags() & MapWorld.FLAG_EDITING) != 0));
            if (removed == null) return;
            event.getInstance().scheduleNextTick(unused -> Thread.startVirtualThread(() -> {
                // ok to use resultNow because we cannot close a world that is not loaded
                // and a loaded world will always have a completed future.
                removed.resultNow().close();
            }));

        });
    }

    public @Blocking void joinMap(@NotNull Player player, @NotNull MapData map, boolean isEditing) {
        var activeWorld = activeMaps.get(new Pair<>(map.id(), isEditing));

        // Create a new world if there is not one present
        if (activeWorld == null) {
            var world = isEditing ? new EditingMapWorld(server, map) : new PlayingMapWorld(server, map);
            activeWorld = VIRTUAL_EXECUTOR.submit(() -> {
                world.load();
                return world;
            });
            activeMaps.put(new Pair<>(map.id(), isEditing), activeWorld);
        }

        // Spawn player in world with loading screen (todo this should be blindness + stop player from moving i guess)
        try {
            var world = activeWorld.get(); // wait for the world to load

            // spawn in minestom instance & then notify world
            player.setInstance(world.instance(), world.spawnPoint()).join();
            world.acceptPlayer(player);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to load world", e);
            throw new RuntimeException(e);
        }
    }
}
