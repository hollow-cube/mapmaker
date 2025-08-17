package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerTeleportingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TeleportHistoryFeatureProvider implements FeatureProvider {

    public static final Tag<Point> LAST_LOCATION = Tag.Transient("teleport_history:last_location");

    private final EventNode<InstanceEvent> events = EventNode.type("teleport-history-event-node", EventFilter.INSTANCE)
            .addListener(MapPlayerTeleportingEvent.class, this::onTeleport);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof EditingMapWorld)) return false;

        world.eventNode().addChild(events);

        return true;
    }

    private void onTeleport(MapPlayerTeleportingEvent event) {
        event.player().setTag(LAST_LOCATION, event.player().getPosition());
    }


}
