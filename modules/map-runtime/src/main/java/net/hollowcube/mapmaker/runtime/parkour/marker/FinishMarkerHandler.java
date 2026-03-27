package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.minestom.server.entity.Player;

public class FinishMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:finish";

    public FinishMarkerHandler(MarkerEntity entity) {
        super(ID, entity);
    }

    @Override
    public void onPlayerEnter(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        // Note that we only check playing here, not testing.
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing2(var saveState)))
            return;

        var finishState = saveState.copy(saveState.state(PlayState.class));
        finishState.complete(System.nanoTime() / 1_000_000);
        finishState.setEndLatency(((MapPlayer) player).averageLatency());
        world.changePlayerState(player, new ParkourState.Finished(finishState));
    }

}
