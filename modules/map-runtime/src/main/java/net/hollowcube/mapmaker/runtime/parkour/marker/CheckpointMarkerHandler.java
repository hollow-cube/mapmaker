package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckpointMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:checkpoint";

    private CheckpointEffectDataV2 data;

    public CheckpointMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.data = entity.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
    }

    @Override
    public void onPlayerEnter(@NotNull Player player) {
        if (this.data == null) return;

        TempEffectApplicator.applyTo(data, player, entity.getUuid().toString());
    }
}
