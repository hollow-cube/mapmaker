package net.hollowcube.mapmaker.map.feature.play.setting;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeServerRulesPacket;
import net.hollowcube.compat.noxesium.rules.NoxesiumServerRules;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class NoTurnFeatureProvider extends AbstractSettingFeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/noturn", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerUpdateStateEvent.class, this::playerUpdated)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::removePlayer)
            .addListener(PlayerMoveEvent.class, this::playerMoved);

    @Override
    protected EventNode<InstanceEvent> getEvents() {
        return eventNode;
    }

    private static boolean canTurn(@NotNull Player player, MapWorld world) {
        if (!world.isPlaying(player)) return true;

        var state = SaveState.fromPlayer(player);
        var playstate = state.state(PlayState.class);
        return !playstate.settings().get(MapSettings.NO_TURN, world.map().settings());
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.player();
        if (!event.mapWorld().isPlaying(player)) return;
        if (canTurn(player, event.mapWorld())) return;
        if (!event.isMapJoin()) return;

        player.sendMessage(Component.translatable("map.join.warning.setting.no_turn"));
    }

    public void playerMoved(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;
        if (canTurn(player, world)) return;

        var newPos = event.getNewPosition();
        var oldPos = event.getPlayer().getPosition();

        if (newPos.yaw() != oldPos.yaw() || newPos.pitch() != oldPos.pitch()) {
            EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        }
    }

    public void playerUpdated(@NotNull MapPlayerUpdateStateEvent event) {
        updatePlayer(event.player());
    }

    public void removePlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        setNoxesiumNoTurn(event.player(), false);
    }

    private void updatePlayer(@NotNull Player player) {
        var world = MapWorld.forPlayer(player);
        var canTurn = canTurn(player, world);
        setNoxesiumNoTurn(player, !canTurn);
    }

    private void setNoxesiumNoTurn(@NotNull Player player, boolean noTurn) {
        ClientboundChangeServerRulesPacket.builder().add(NoxesiumServerRules.CAMERA_LOCKED, noTurn).build().send(player);
    }
}
