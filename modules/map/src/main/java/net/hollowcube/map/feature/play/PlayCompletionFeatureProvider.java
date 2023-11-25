package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.util.FireworkUtil;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Future;

import static net.hollowcube.map.feature.play.TimerFeatureProvider.formatPlaytime;

@AutoService(FeatureProvider.class)
public class PlayCompletionFeatureProvider implements FeatureProvider {

    private static final Tag<Future<SaveState>> BEST_SAVE_STATE_TAG = Tag.Transient("map:best_save_state");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enable this feature if the world is playing _and not testing_
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0 || (world.flags() & MapWorld.FLAG_TESTING) != 0)
            return false;

        var eventNode = EventNode.type("map-completion/play", EventFilter.INSTANCE);
        eventNode.addListener(MapPlayerInitEvent.class, this::handlePlayerInit);
        eventNode.addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerRemove);
        eventNode.addListener(MapWorldCompleteEvent.class, FutureUtil.virtual(this::handleMapCompletion));
        world.addScopedEventNode(eventNode);

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

    private @Blocking void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        var world = (InternalMapWorld) MapWorld.forPlayer(player);

        var saveState = SaveState.fromPlayer(player);
        saveState.setCompleted(true); // Also stops recording time here
        var finishFuture = player.getTag(BEST_SAVE_STATE_TAG); // Fetch now because it will be removed when player is removed from world.

        // Remove the player from the world itself, they are no longer playing (but will remain in the instance)
        // This will also cause their savestate to be written to DB
        world.removePlayer(player);
        if (world instanceof PlayingMapWorld pmw) {
            pmw.startFinished(player, false);
        }

        // Show the completed message after removing the player because it is theoretically possible to not have the savestate fetched yet.
        var bestSaveState = FutureUtil.getUnchecked(finishFuture);
        if (bestSaveState == null) {
            player.sendMessage(Component.translatable("map.completed.first", Component.text(formatPlaytime(saveState.getPlaytime()))));
        } else {
            var diffPlaytime = bestSaveState.getPlaytime() - saveState.getPlaytime();
            player.sendMessage(Component.translatable("map.completed.with_prior",
                    Component.text(formatPlaytime(saveState.getPlaytime())),
                    Component.text((diffPlaytime < 0 ? "+" : "-") + formatPlaytime(Math.abs(diffPlaytime)), diffPlaytime < 0 ? NamedTextColor.RED : NamedTextColor.GREEN)));
        }

        FireworkUtil.showFirework(event.getPlayer(), event.getInstance(), event.getPlayer().getPosition(), 15, List.of(FireworkUtil.randomColorEffect()));
    }

}
