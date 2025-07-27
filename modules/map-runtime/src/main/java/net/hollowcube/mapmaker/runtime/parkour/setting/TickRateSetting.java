package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.server.play.SetTickStatePacket;

import java.util.Objects;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class TickRateSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, TickRateSetting::updatePlayer);

    public static int getTickRate(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return ServerFlag.SERVER_TICKS_PER_SECOND;

        return getTickRate(world, p.saveState().state(PlayState.class));
    }

    public static int getTickRate(ParkourMapWorld world, PlayState playState) {
        return Objects.requireNonNullElse(playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.TICK_RATE, world.map().settings()), ServerFlag.SERVER_TICKS_PER_SECOND);
    }

    private static void updatePlayer(ParkourMapPlayerUpdateStateEvent event) {
        final var world = event.world();
        final var player = event.player();

        final int tickRate = event.isReset() ? ServerFlag.SERVER_TICKS_PER_SECOND : getTickRate(world, player);
        player.sendPacket(new SetTickStatePacket(tickRate, false));
    }

}
