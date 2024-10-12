package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStopSprintingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * {@link NoSneakFeatureProvider} is also enabled to prevent sneaking in only sprint maps.
 */
@AutoService(FeatureProvider.class)
public class OnlySprintFeatureProvider implements FeatureProvider {
    private static final Tag<Point> ONLY_SPRINT_TAG = Tag.Transient("onlysprint");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/onlysprint", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(PlayerStopSprintingEvent.class, this::onStopSprinting)
            .addListener(PlayerMoveEvent.class, this::onPlayerMove);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || !settings.isOnlySprint())
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        player.setTag(ONLY_SPRINT_TAG, player.getPosition());
        if (event.isMapJoin()) {
            player.sendMessage(Component.translatable("map.join.warning.setting.only_sprint"));
        }
    }

    public void onStopSprinting(@NotNull PlayerStopSprintingEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        if (!world.isPlaying(player)) return;

        player.removeTag(ONLY_SPRINT_TAG);

        EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        //todo sound effect for sprint stopped
    }

    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        if (!world.isPlaying(player)) return;
        if (player.isSprinting() || !player.hasTag(ONLY_SPRINT_TAG)) return;

        var startPos = player.getTag(ONLY_SPRINT_TAG);
        var newPos = event.getNewPosition();
        if (startPos.distanceSquared(newPos) < 1) return;

        // They moved >1 block before starting sprinting, reset them
        player.removeTag(ONLY_SPRINT_TAG);
        EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        //todo sound effect for sprint stopped
    }
}
