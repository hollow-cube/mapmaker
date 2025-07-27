package net.hollowcube.mapmaker.map.feature.play.vanilla;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.item.vanilla.EnderPearlItem;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.item.vanilla.WindChargeItem;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class ElytraFeatureProvider implements FeatureProvider {
    public static final Tag<Boolean> IS_GLIDING_TAG = Tag.Transient("mapmaker:elytra_gliding");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("elytra-feature-provider", EventFilter.INSTANCE)
            .addListener(PlayerStartFlyingWithElytraEvent.class, this::handleStartFlying)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::handleStopPlaying)
            .addListener(PlayerStopFlyingWithElytraEvent.class, this::handleStopFlying);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.instance().eventNode().addChild(eventNode);
        world.itemRegistry().registerSilent(FireworkRocketItem.INSTANCE);
        world.itemRegistry().registerSilent(EnderPearlItem.INSTANCE);
        world.itemRegistry().registerSilent(WindChargeItem.INSTANCE);

        return true;
    }

    private void handleStopPlaying(@NotNull MapWorldPlayerStopPlayingEvent event) {
        FireworkRocketItem.removeRocket(event.getPlayer());
    }

    private void handleStartFlying(@NotNull PlayerStartFlyingWithElytraEvent event) {
        event.getPlayer().setTag(IS_GLIDING_TAG, true);
    }

    private void handleStopFlying(@NotNull PlayerStopFlyingWithElytraEvent event) {
        event.getPlayer().removeTag(IS_GLIDING_TAG);
        FireworkRocketItem.removeRocket(event.getPlayer());
    }
}
