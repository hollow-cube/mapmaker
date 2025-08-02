package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatusMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:status";

    private StatusEffectData data;

    public StatusMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.data = entity.getTag(StatusPlateBlock.ENTITY_DATA_TAG);
    }

    @Override
    public void onPlayerEnter(@NotNull Player player) {
        if (this.data == null) return;
        if (!StatusPlateBlock.APPLY_COOLDOWN.test(player)) return;

        TempEffectApplicator.applyTo(data, player, entity.getUuid().toString());
    }
}
