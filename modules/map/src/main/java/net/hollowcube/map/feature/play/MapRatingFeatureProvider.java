package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

@AutoService(FeatureProvider.class)
public class MapRatingFeatureProvider implements FeatureProvider {
    public static final Tag<Future<Integer>> LAST_RATING_TAG = Tag.Transient("map:last_rating");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/rating", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handlePlayerInit)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerRemove);

    public static boolean isMapRatable(@NotNull MapWorld world) {
        return world.flags() == MapWorld.FLAG_PLAYING;
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Match on play only, not testing.
        if (!isMapRatable(world))
            return false;

        world.addScopedEventNode(eventNode);
        return true;
    }

    private void handlePlayerInit(@NotNull MapPlayerInitEvent event) {
        if (!event.isFirstInit()) return;


        var player = event.getPlayer();
        var world = event.getMapWorld();

        var future = MapService.VIRTUAL_EXECUTOR.submit(() -> {
            try {
                return world.server().mapService().getMapRating(world.map().id(), player.getUuid().toString());
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                return -1;
            }
        });
        player.setTag(LAST_RATING_TAG, future);
    }

    private void handlePlayerRemove(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.getPlayer().removeTag(LAST_RATING_TAG);
    }

}
