package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.gui.RateMapView;
import net.hollowcube.mapmaker.map.util.FireworkUtil;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.MapRating;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Future;

import static net.hollowcube.mapmaker.feature.FeatureFlag.player;
import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

@AutoService(FeatureProvider.class)
public class PlayCompletionFeatureProvider implements FeatureProvider {

    private static final Tag<Future<SaveState>> BEST_SAVE_STATE_TAG = Tag.Transient("map:best_save_state");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld))
            return false;

        var eventNode = EventNode.type("map-completion/play", EventFilter.INSTANCE);
        eventNode.addListener(MapPlayerInitEvent.class, this::handlePlayerInit);
        eventNode.addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerRemove);
        eventNode.addListener(MapPlayerCompleteMapEvent.class, this::handleMapCompletion);
        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handlePlayerInit(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;

        var player = event.getPlayer();
        var world = event.getMapWorld();

        var future = MapService.VIRTUAL_EXECUTOR.submit(() -> world.server().mapService()
                .getBestSaveState(world.map().id(), player.getUuid().toString()));
        player.setTag(BEST_SAVE_STATE_TAG, future);
    }

    private void handlePlayerRemove(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.getPlayer().removeTag(BEST_SAVE_STATE_TAG);
    }

    private void handleMapCompletion(@NotNull MapPlayerCompleteMapEvent event) {
        var player = event.getPlayer();
        var world = (PlayingMapWorld) event.getMapWorld(); // Safe because this is only enabled on playing worlds.

        var saveState = SaveState.fromPlayer(player);
        saveState.setCompleted(true); // Also stops recording time here
        var finishFuture = player.getTag(BEST_SAVE_STATE_TAG); // Fetch now because it will be removed when player is removed from world.

        // Pre-remove the playing tags
        //todo this is a bad solution. Basically we need to remove the player immediately, but the remove method runs in a virtual thread
        // which means it will have a tiny scheduling delay which means duplicates can trigger. To get around this we just remove these
        // two tags immediately which will stop them from triggering new events. Its a terrible solution and needs to be reworked.
        world.removePlayerImmediate(player);

        FutureUtil.submitVirtual(() -> {
            // Remove the player from the world itself, they are no longer playing (but will remain in the instance)
            // This will also cause their savestate to be written to DB
            // Then re-add the player to the world as a spectator (in finished mode)

            var resp = world.removeActivePlayer(player);
            world.addSpectator(player, true);

            // Show the completed message after removing the player because it is theoretically possible to not have the savestate fetched yet.
            var bestSaveState = FutureUtil.getUnchecked(finishFuture);
            if (bestSaveState == null) {
                player.sendMessage(Component.translatable("map.completed.first", Component.text(formatMapPlaytime(saveState.getPlaytime(), true))));
            } else {
                // Diff playtime rounded to ticks prior to subtracting for correct display.
                var diffPlaytime = (bestSaveState.getPlaytime() - bestSaveState.getPlaytime() % MinecraftServer.TICK_MS) - (saveState.getPlaytime() - saveState.getPlaytime() % MinecraftServer.TICK_MS);
                player.sendMessage(Component.translatable("map.completed.with_prior",
                        Component.text(formatMapPlaytime(saveState.getPlaytime(), true)),
                        // Note: roundToTicks is not used here. We do the rounding above because we need to round prior to calculating the difference.
                        Component.text((diffPlaytime < 0 ? "+" : "-") + formatMapPlaytime(Math.abs(diffPlaytime), false), diffPlaytime < 0 ? NamedTextColor.RED : NamedTextColor.GREEN)));
            }

//            player.showTitle(Title.title(Component.text("Map Completed", NamedTextColor.GOLD), Component.empty(), Title.Times.of(Duration.ZERO, Duration.of(80, TimeUnit.SERVER_TICK), Duration.ZERO)));
//            player.sendPacket(new SetTickStatePacket(15, false));
//            MinecraftServer.getSchedulerManager().submitTask(new Supplier<>() {
//                private int state = 0;
//
//                @Override
//                public TaskSchedule get() {
//                    if (state < 10) {
//                        var newRate = (float) CoordinateUtil.lerp(15.0, 4.0, (float) (state + 1) / 10f);
//                        player.sendPacket(new SetTickStatePacket(newRate, false));
//                        state++;
//                        return TaskSchedule.tick(3);
//                    }
//                    if (state == 10) {
//                        state++;
//                        return TaskSchedule.tick(50);
//                    }
//
//                    player.sendPacket(new SetTickStatePacket(20, false));
//                    return TaskSchedule.stop();
//                }
//            });

            FireworkUtil.showFirework(event.getPlayer(), event.getInstance(), event.getPlayer().getPosition(), 15, List.of(FireworkUtil.randomColorEffect()));

            // Show the review GUI for the player if they have not submitted a rating yet
            if (MapFeatureFlags.RATE_MAP.test(player(player))) {
                if (MapRatingFeatureProvider.isMapRatable(world)) {
                    var lastRating = FutureUtil.getUnchecked(player.getTag(MapRatingFeatureProvider.LAST_RATING_TAG));
                    if (lastRating == null || lastRating.state() == MapRating.State.UNRATED) {
                        world.server().showView(player, c -> new RateMapView(c, world.map().id()));
                    }
                }
            }
        });
    }

}
