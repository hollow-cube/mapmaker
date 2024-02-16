package net.hollowcube.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.event.MapPlayerInitEvent;
import net.hollowcube.map2.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoSprintFeatureProvider implements FeatureProvider {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/nosprint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isNoSprint())
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.setFood(6);
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();

        player.setFood(20);
    }
}
