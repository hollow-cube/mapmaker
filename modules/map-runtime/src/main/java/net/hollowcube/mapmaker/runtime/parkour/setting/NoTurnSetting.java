package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class NoTurnSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, NoTurnSetting::updatePlayer)
            .addListener(PlayerMoveEvent.class, NoTurnSetting::playerMoved);

    public static boolean canTurn(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canTurn(world, p.saveState().state(PlayState.class));
    }

    public static boolean canTurn(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_TURN, world.map().settings());
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        final var world = event.world();
        final var player = event.player();

        var canTurn = event.isReset() || canTurn(world, event.player());
        if (event.isMapJoin() && !canTurn) {
            player.sendMessage(Component.translatable("map.join.warning.setting.no_turn"));
        }

        NoxesiumPlayer.get(player).set(NoxesiumGameComponents.CAMERA_LOCKED, !canTurn);
    }

    private static void playerMoved(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || canTurn(world, player)) return;

        var newPos = event.getNewPosition();
        var oldPos = event.getPlayer().getPosition();
        if (oldPos.sameView(newPos)) return;

        world.softResetPlayer(player);
    }

}
