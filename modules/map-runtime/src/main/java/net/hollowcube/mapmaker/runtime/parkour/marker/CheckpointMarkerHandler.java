package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.block.CheckpointPlateBlock;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CheckpointMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:checkpoint";

    private @Nullable ActionTriggerData data;

    public CheckpointMarkerHandler(MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.data = entity.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (this.data == null) return;

        TempEffectApplicator.applyCheckpoint(data, player, entity.getUuid().toString());
    }

    @Override
    public void onPlayerExit(Player player) {
        if (this.data == null) return;

        TempEffectApplicator.handleCheckpointExit(player, data, entity.getUuid().toString());
    }
}
