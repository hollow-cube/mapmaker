package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import org.jetbrains.annotations.NotNull;

@AutoService(ExternalDataFix.class)
public class V4438 extends DataVersion implements ExternalDataFix {

    public V4438() {
        super(4438);

        addFix(DataTypes.ENTITY, "minecraft:marker", V4438::updateEffectMarker);
        addFix(HCDataTypes.WORLD, V4438::updateWorldSpawnCheckpoint);
    }

    private static Value updateWorldSpawnCheckpoint(@NotNull Value worldData) {
        var spawnCheckpoint = worldData.get("spawn_checkpoint_effects");
        if (!spawnCheckpoint.isNull()) V4437.updateItemActionPlaceableOn(spawnCheckpoint);
        return null;
    }

    private static Value updateEffectMarker(@NotNull Value entity) {
        var data = entity.get("data");
        var type = data.get("type").as(String.class, "");
        if ("mapmaker:checkpoint".equals(type)) {
            V4437.updateItemActionPlaceableOn(data.get("checkpoint"));
        } else if ("mapmaker:status".equals(type)) {
            V4437.updateItemActionPlaceableOn(data.get("status"));
        }
        return null;
    }

}
