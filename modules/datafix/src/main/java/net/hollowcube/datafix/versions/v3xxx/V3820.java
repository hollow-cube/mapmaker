package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V3820 extends DataVersion {
    public V3820() {
        super(3820);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:skull", V3820::fixSkullBlockEntity);
        addFix(DataTypes.DATA_COMPONENTS, V3820::fixLodestoneTrackerCompass);
    }

    private static @Nullable Value fixSkullBlockEntity(Value blockEntity) {
        blockEntity.remove("ExtraType");
        blockEntity.put("profile", V3818_5.fixProfile(blockEntity.remove("SkullOwner")));
        return null;
    }

    private static @Nullable Value fixLodestoneTrackerCompass(Value dataComponents) {
        var lodestoneTracker = dataComponents.get("minecraft:lodestone_tracker");
        if (!lodestoneTracker.isMapLike()) return null;

        var pos = lodestoneTracker.remove("pos");
        var dimension = lodestoneTracker.remove("dimension");
        if (!pos.isNull() && !dimension.isNull()) {
            var target = Value.emptyMap();
            target.put("pos", pos);
            target.put("dimension", dimension);
            lodestoneTracker.put("target", target);
        }

        return null;
    }

}
