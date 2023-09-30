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
import net.minestom.server.event.player.PlayerStopSprintingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class OnlySprintFeatureProvider implements FeatureProvider {
    private static final Tag<Point> ONLY_SPRINT_TAG = Tag.Transient("onlysprint");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/onlysprint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(PlayerStopSprintingEvent.class, this::onStopSprinting)
            .addListener(PlayerMoveEvent.class, this::onPlayerMove);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & (MapWorld.FLAG_PLAYING|MapWorld.FLAG_TESTING)) == 0)
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isOnlySprint())
            return false;

        world.addScopedEventNode(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.setTag(ONLY_SPRINT_TAG, player.getPosition());
    }

    public void onStopSprinting(@NotNull PlayerStopSprintingEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.removeTag(ONLY_SPRINT_TAG);

        var world = MapWorld.forPlayer(player);
        EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
        //todo sound effect for sprint stopped
    }

    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;
        if (player.isSprinting() || !player.hasTag(ONLY_SPRINT_TAG)) return;

        var startPos = player.getTag(ONLY_SPRINT_TAG);
        var newPos = event.getNewPosition();
        if (startPos.distanceSquared(newPos) < 1) return;

        // They moved >1 block before starting sprinting, reset them
        player.removeTag(ONLY_SPRINT_TAG);
        var world = MapWorld.forPlayer(player);
        EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
        //todo sound effect for sprint stopped
    }
}
