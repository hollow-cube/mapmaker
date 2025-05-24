package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.SetTickStatePacket;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TickRateFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/tickrate", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerUpdateStateEvent.class, this::playerUpdated)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static int getTickRate(@NotNull Player player, @NotNull MapWorld world) {
        if (!world.isPlaying(player)) return ServerFlag.SERVER_TICKS_PER_SECOND;

        var state = SaveState.fromPlayer(player);
        var playstate = state.state(PlayState.class);
//        return Objects.requireNonNullElse(playstate.settings().get(MapSettings.TICK_RATE, world.map().settings()), ServerFlag.SERVER_TICKS_PER_SECOND);
        return 20; // todo
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.mapWorld().isPlaying(player)) return;
        if (!event.isMapJoin()) return;
        updatePlayer(player);
    }

    public void playerUpdated(@NotNull MapPlayerUpdateStateEvent event) {
        updatePlayer(event.player());
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        event.player().sendPacket(new SetTickStatePacket(ServerFlag.SERVER_TICKS_PER_SECOND, false));
    }

    private void updatePlayer(@NotNull Player player) {
        var world = MapWorld.forPlayer(player);
        player.sendPacket(new SetTickStatePacket(getTickRate(player, world), false));
    }
}
