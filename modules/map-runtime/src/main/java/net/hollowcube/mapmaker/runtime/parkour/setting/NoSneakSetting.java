package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class NoSneakSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, NoSneakSetting::initPlayer)
            .addListener(PlayerMoveEvent.class, NoSneakSetting::playerMoved);

    public static boolean canSneak(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canSneak(world, p.saveState().state(PlayState.class));
    }

    public static boolean canSneak(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_SNEAK, world.map().settings());
    }

    private static void initPlayer(ParkourMapPlayerUpdateStateEvent event) {
        var player = event.player();
        if (!event.isMapJoin() || canSneak(event.world(), player))
            return; // Nothing to warn

        player.sendMessage(Component.translatable("map.join.warning.setting.no_sneak"));
    }

    private static void playerMoved(PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (!player.isSneaking()) return;

        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || canSneak(world, player)) return;

        // Player is sneaking, but if they only turn their head its OK.
        var newPos = event.getNewPosition();
        var oldPos = event.getPlayer().getPosition();
        if (oldPos.samePoint(newPos, Vec.EPSILON)) return;

        world.softResetPlayer(player);
    }

}
