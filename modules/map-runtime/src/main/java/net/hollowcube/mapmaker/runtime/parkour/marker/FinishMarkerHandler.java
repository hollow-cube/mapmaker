package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.node.LegacyTriggers;
import net.minestom.server.entity.Player;

public class FinishMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:finish";

    public FinishMarkerHandler(MarkerEntity entity) {
        super(ID, entity);
    }

    @Override
    public void onPlayerEnter(Player player) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        LegacyTriggers.finish(entity.getUuid().toString(), entity.getPosition())
            .onEnter(world, player);
    }

}
