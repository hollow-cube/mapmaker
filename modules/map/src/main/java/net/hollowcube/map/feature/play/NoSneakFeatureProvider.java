package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapPlayerResetTriggerEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerStopSprintingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoSneakFeatureProvider implements FeatureProvider {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:player/nosneak", EventFilter.INSTANCE)
            .addListener(PlayerStartSneakingEvent.class, this::onStartSneaking);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & (MapWorld.FLAG_PLAYING|MapWorld.FLAG_TESTING)) == 0)
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isNoSneak())
            return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    public void onStartSneaking(@NotNull PlayerStartSneakingEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        var world = MapWorld.forPlayer(player);
        EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
        //todo sound effect for sneaking
        player.sendMessage("No sneaking!");
    }
}
