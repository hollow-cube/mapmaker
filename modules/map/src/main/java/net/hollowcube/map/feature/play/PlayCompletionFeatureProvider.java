package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.gui.CompletedMapView;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class PlayCompletionFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enable this feature if the world is playing _and not testing_
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0 || (world.flags() & MapWorld.FLAG_TESTING) != 0)
            return false;

        var eventNode = EventNode.type("map-completion/play", EventFilter.INSTANCE);
        eventNode.addListener(MapWorldCompleteEvent.class, FutureUtil.virtual(this::handleMapCompletion));
        world.addScopedEventNode(eventNode);

        return true;
    }

    private @Blocking void handleMapCompletion(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        var world = (InternalMapWorld) MapWorld.forPlayer(player);

        var saveState = SaveState.fromPlayer(player);
        saveState.setCompleted(true);

        world.server().newOpenGUI(player, CompletedMapView::new);

        // Remove the player from the world itself, they are no longer playing (but will remain in the instance)
        // This will also cause their savestate to be written to DB
        world.removePlayer(player);
        if (world instanceof PlayingMapWorld pmw) {
            pmw.startSpectating(player, false);
        }

        player.setGameMode(GameMode.SPECTATOR);
    }

}
