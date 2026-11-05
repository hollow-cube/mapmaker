package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.block.StatusPlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.node.LegacyTriggers;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNode;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class StatusMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:status";

    private @Nullable TriggerNode trigger;

    public StatusMarkerHandler(MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        updateTrigger(entity.getPosition());
    }

    @Override
    public void onPlayerEnter(Player player) {
        var world = MapWorld.forPlayer(player);
        if (trigger != null && world != null) trigger.onEnter(world, player);
    }

    private void updateTrigger(Pos position) {
        var data = entity.getTag(StatusPlateBlock.ENTITY_DATA_TAG);
        if (data == null) {
            this.trigger = null;
            return;
        }

        this.trigger = LegacyTriggers.status(entity.getUuid().toString(), position, data);
    }
}
