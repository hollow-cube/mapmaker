package net.hollowcube.mapmaker.runtime.parkour.setting;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class ResetLiquidSetting {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(PlayerTickEvent.class, ResetLiquidSetting::handlePlayerTick);

    public static boolean canGoInWater(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canGoInWater(world, p.saveState().state(PlayState.class));
    }

    public static boolean canGoInWater(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.RESET_IN_WATER, world.map().settings());
    }

    public static boolean canGoInLava(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return true;

        return canGoInLava(world, p.saveState().state(PlayState.class));
    }

    public static boolean canGoInLava(ParkourMapWorld world, PlayState playState) {
        return !playState.get(Attachments.SETTINGS, SavedMapSettings.EMPTY)
                .get(MapSettings.RESET_IN_LAVA, world.map().settings());
    }

    private static void handlePlayerTick(PlayerTickEvent event) {
        if (!(event.getPlayer() instanceof MapPlayer player)) return;
        if (!(player.isInWater() || player.isInLava())) return;

        var world = ParkourMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;

        var playState = p.saveState().state(PlayState.class);
        boolean isWaterReset = !canGoInWater(world, playState) && player.isInWater();
        boolean isLavaReset = isWaterReset || !canGoInLava(world, playState) && player.isInLava();
        if (isWaterReset || isLavaReset) world.softResetPlayer(player);
    }

}
