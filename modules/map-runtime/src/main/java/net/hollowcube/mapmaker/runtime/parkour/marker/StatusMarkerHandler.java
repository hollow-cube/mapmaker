package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.block.StatusPlateBlock;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class StatusMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:status";

    private @Nullable ActionTriggerData data;

    public StatusMarkerHandler(MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.data = entity.getTag(StatusPlateBlock.ENTITY_DATA_TAG);
    }

    @Override
    public void onPlayerEnter(Player player) {
        if (this.data == null) return;
        if (!StatusPlateBlock.APPLY_COOLDOWN.test(player)) return;

        TempEffectApplicator.applyStatus(data, player, entity.getUuid().toString());
    }
}
