package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoSneakFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:player/nosneak", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::onPlayerInit)
            .addListener(PlayerMoveEvent.class, this::onPlayerMove);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static boolean canSneak(@NotNull Player player, @NotNull MapWorld world) {
        var state = SaveState.optionalFromPlayer(player);
        if (state == null) return true;

        var playstate = state.state(PlayState.class);
        return !playstate.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_SNEAK, world.map().settings());
    }

    public void onPlayerInit(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;
        if (canSneak(player, event.getMapWorld())) return;
        if (!event.isMapJoin()) return;

        player.sendMessage(Component.translatable("map.join.warning.setting.no_sneak"));
    }

    //todo(matt): not a big fan of using the move event here, but i don't know any other way
    //            since you cannot tell the client to stop sneaking from the server
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player) || !player.isSneaking()) return;
        if (canSneak(player, world)) return;

        // Player is sneaking, reset them if this move is anything besides a head look
        if (Vec.fromPoint(event.getNewPosition()).equals(Vec.fromPoint(player.getPosition()))) return;

        EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        //todo sound effect for sneaking
//        player.sendMessage("No sneaking!");
    }
}
