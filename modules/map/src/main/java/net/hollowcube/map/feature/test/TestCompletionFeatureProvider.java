package net.hollowcube.map.feature.test;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TestCompletionFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enable this feature if the world is testing _and not playing
        if ((world.flags() & MapWorld.FLAG_TESTING) == 0)
            return false;

        System.out.println("init for world " + world);
        var eventNode = EventNode.type("map-completion/test", EventFilter.INSTANCE);
        eventNode.addListener(MapWorldCompleteEvent.class, FutureUtil.virtual(this::handleMapCompletion));
        world.addScopedEventNode(eventNode);

        return true;
    }

    private @Blocking void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        var world = (TestingMapWorld) MapWorld.forPlayer(player);

        // Not sure what should really happen here, for now just tell them
        // they completed the map and send them back to editing mode
        player.sendMessage("Completed map");
        world.exitTestMode(player);
    }

}
