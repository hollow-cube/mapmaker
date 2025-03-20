package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapHooks;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BoatFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/boat", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerCompleteMapEvent.class, this::finishOrSpectateMap)
            .addListener(MapPlayerStartSpectatorEvent.class, this::finishOrSpectateMap);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        if (world.map().settings().getVariant() != MapVariant.PARKOUR || !world.map().getSetting(MapSettings.BOAT))
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        new BoatEntity(player);
    }

    private void finishOrSpectateMap(@NotNull PlayerEvent event) {
        var player = event.getPlayer();

        var vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.removePassenger(player);
            vehicle.remove();
            player.teleport(player.getPosition().add(0, 0.5, 0)); // Need to teleport up or they fall through floors
        }
    }

    private static class BoatEntity extends Entity {

        public BoatEntity(Player player) {
            super(EntityType.OAK_BOAT);

            this.setTag(MapHooks.ASSOCIATED_PLAYER, player);
            FutureUtil.getUnchecked(this.setInstance(player.getInstance(), player.getPosition().add(0, 0.25, 0)));
            this.addPassenger(player);
        }

        @Override
        public void tick(long time) {
            super.tick(time);

            if (this.getPassengers().isEmpty()) {
                this.remove();
            }
        }
    }
}
