package net.hollowcube.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BoatFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/boat", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldCompleteEvent.class, this::finishMap);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & (MapWorld.FLAG_PLAYING | MapWorld.FLAG_TESTING)) == 0)
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isBoat())
            return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    private void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        var boat = new Entity(EntityType.BOAT);
        boat.setTag(MapHooks.ASSOCIATED_PLAYER, player);
        boat.setInstance(player.getInstance(), player.getPosition()).join();
        boat.addPassenger(player);
    }

    private void finishMap(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();

        var vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.removePassenger(player);
            vehicle.remove();
        }
    }
}
