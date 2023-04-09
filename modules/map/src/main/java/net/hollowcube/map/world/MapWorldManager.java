package net.hollowcube.map.world;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import kotlin.Pair;
import net.hollowcube.map.MapServer;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("UnstableApiUsage")
public class MapWorldManager {

    private final Map<Pair<String, Boolean>, ListenableFuture<InternalMapWorldNew>> activeMaps = new ConcurrentHashMap<>();
    private final MapServer server;

    public MapWorldManager(@NotNull MapServer server) {
        this.server = server;

        MinecraftServer.getGlobalEventHandler().addListener(PlayerInstanceLeaveEvent.class, event -> {
            // Ignore if there are still players in the instance
            if (event.getInstance().getPlayers().size() > 1) return;

            var world = MapWorldNew.optionalFromInstance(event.getInstance());
            if (world == null) return;

            var removed = activeMaps.remove(new Pair<>(world.map().getId(), (world.flags() & MapWorldNew.FLAG_EDITING) != 0));
            if (removed == null) return;
            event.getInstance().scheduleNextTick(unused -> {
                try {
                    Futures.addCallback(removed.get().close(), new FutureCallback<>() {
                        @Override
                        public void onSuccess(Void result) {

                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                        }
                    }, Runnable::run);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    public @NotNull ListenableFuture<Void> joinMap(@NotNull Player player, @NotNull MapData map, boolean isEditing) {
        var activeWorld = activeMaps.get(new Pair<>(map.getId(), isEditing));

        if (activeWorld == null) {
            // Create a new world
            var world = new EditingMapWorldNew(server, map);
            activeWorld = Futures.transform(world.load(), unused -> world, Runnable::run);
            activeMaps.put(new Pair<>(map.getId(), isEditing), activeWorld);
        }

        // Spawn the player in the world.
        return Futures.transformAsync(activeWorld, world -> {
            // Start acceptPlayer future, which will load save state/etc
            var f = world.acceptPlayer(player);

            // Spawn player in world with loading screen (todo this should be blindness + stop player from moving i guess)
            var spawnFuture = JdkFutureAdapters.listenInPoolThread(player.setInstance(world.instance(), world.spawnPoint()));
            return Futures.transformAsync(spawnFuture, unused -> {
//                if (!f.isDone()) {
//                    player.showTitle(Title.title(Component.text("Loading..."), Component.text(""),
//                            Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(10000), Duration.ofSeconds(0))));
//                }
                return Futures.transform(f, unused2 -> null, Runnable::run);
            }, Runnable::run);
        }, Runnable::run);
    }
}
