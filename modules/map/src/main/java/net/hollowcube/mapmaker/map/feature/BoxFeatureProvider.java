package net.hollowcube.mapmaker.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapType;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BoxFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map:box", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, this::handleBlockBreak)
            .addListener(PlayerBlockPlaceEvent.class, this::handleBlockPlace);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!world.map().type().equals(MapType.BOX))
            return false;

        if (!(world instanceof EditingMapWorld))
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handleBlockPlace(@NotNull PlayerBlockPlaceEvent event) {
        // TODO block placing outside boundary or 2 blocks above start/end
    }

    private void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        // TODO block breaking disallowed from boundary and replace start and end with block in hand
    }
}
