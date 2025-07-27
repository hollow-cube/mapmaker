package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class NoRelogSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, NoRelogSetting::initPlayer);

    public static boolean canRelog(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canRelog(world, p.saveState().state(PlayState.class));
    }

    public static boolean canRelog(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.NO_RELOG, world.map().settings());
    }

    private static void initPlayer(ParkourMapPlayerUpdateStateEvent event) {
        var player = event.player();
        if (!event.isMapJoin() || canRelog(event.world(), player))
            return; // Nothing to warn

        player.sendMessage(Component.translatable("map.join.warning.setting.no_relog"));
        event.world().softResetPlayer(player);
    }

}
