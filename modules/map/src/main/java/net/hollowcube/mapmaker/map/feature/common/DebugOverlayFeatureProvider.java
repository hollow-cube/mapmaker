package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.debug.PlayingDebugOverlay;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class DebugOverlayFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        world.eventNode()
                .addListener(MapPlayerInitEvent.class, this::handlePlayerInit);
        return true;
    }

    public void handlePlayerInit(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();

        if (MapFeatureFlags.DEBUG_PLAYING_OVERLAY.test(player)) {
            ActionBar.forPlayer(player).addProvider(PlayingDebugOverlay.INSTANCE);
        }
    }
}
