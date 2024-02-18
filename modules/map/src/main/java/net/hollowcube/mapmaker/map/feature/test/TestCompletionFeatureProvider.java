package net.hollowcube.mapmaker.map.feature.test;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TestCompletionFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enable this feature if the world is testing _and not playing
        if (!(world instanceof TestingMapWorld))
            return false;

        var eventNode = EventNode.type("map-completion/test", EventFilter.INSTANCE);
        eventNode.addListener(MapPlayerCompleteMapEvent.class, FutureUtil.virtual(this::handleMapCompletion));
        world.eventNode().addChild(eventNode);

        return true;
    }

    private @Blocking void handleMapCompletion(@NotNull MapPlayerCompleteMapEvent event) {
        var player = event.getPlayer();
        var world = (TestingMapWorld) MapWorld.forPlayer(player);

        var map = world.map();
        if (map.verification() == MapVerification.PENDING) {
            // In this case, they just finished verifying the map. congrats to them.
            var saveState = SaveState.fromPlayer(player);
            saveState.setCompleted(true);

            world.server().bridge().joinHub(player);

        } else {
            // Not sure what should really happen here, for now just tell them
            // they completed the map and send them back to editing mode
            player.sendMessage(Component.translatable("testing_mode.finish"));
            world.exitTestMode(player);
        }
    }

}
