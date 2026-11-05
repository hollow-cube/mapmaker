package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.block.CheckpointPlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.node.LegacyTriggers;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNode;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CheckpointMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:checkpoint";

    private @Nullable TriggerNode trigger;

    public CheckpointMarkerHandler(MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        updateTrigger(entity.getPosition());
    }

    @Override
    public void onPositionChange(Pos newPosition) {
        updateTrigger(newPosition);
    }

    @Override
    public void onPlayerEnter(Player player) {
        var world = MapWorld.forPlayer(player);
        if (trigger != null && world != null) trigger.onEnter(world, player);
    }

    @Override
    public void onPlayerExit(Player player) {
        var world = MapWorld.forPlayer(player);
        if (trigger != null && world != null) trigger.onExit(world, player);
    }

    private void updateTrigger(Pos position) {
        var data = entity.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
        if (data == null) {
            this.trigger = null;
            return;
        }

        this.trigger = LegacyTriggers.checkpoint(entity.getUuid().toString(), position, data);
    }
}
