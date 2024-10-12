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
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

// Also enabled for only sprint maps to prevent sneaking
@AutoService(FeatureProvider.class)
public class NoSneakFeatureProvider implements FeatureProvider {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:player/nosneak", EventFilter.INSTANCE)
            .addListener(PlayerMoveEvent.class, this::onPlayerMove);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR || (!settings.isNoSneak() && !settings.isOnlySprint()))
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    public void onPlayerInit(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        if (event.isMapJoin()) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_sneak"));
        }
    }

    //todo(matt): not a big fan of using the move event here, but i don't know any other way
    //            since you cannot tell the client to stop sneaking from the server
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player) || !player.isSneaking()) return;

        // Player is sneaking, reset them if this move is anything besides a head look
        if (Vec.fromPoint(event.getNewPosition()).equals(Vec.fromPoint(player.getPosition()))) return;

        EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        //todo sound effect for sneaking
//        player.sendMessage("No sneaking!");
    }
}
