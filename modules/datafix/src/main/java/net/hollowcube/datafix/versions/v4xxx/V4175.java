package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4175 extends DataVersion {
    public V4175() {
        super(4175);

        addFix(DataTypes.DATA_COMPONENTS, V4175::fixEquippableAssetRename);
        addFix(DataTypes.DATA_COMPONENTS, V4175::fixCustomModelDataExpand);
    }

    private static Value fixEquippableAssetRename(Value dataComponents) {
        var equippable = dataComponents.get("minecraft:equippable");
        if (!equippable.isMapLike()) return null;

        equippable.put("asset_id", equippable.remove("model"));
        return null;
    }

    private static Value fixCustomModelDataExpand(Value dataComponents) {
        var cmd = dataComponents.get("minecraft:custom_model_data");
        var float0 = cmd.as(Number.class, 0.0f).floatValue();

        var floats = Value.emptyList();
        floats.put(float0);
        var expandedCmd = Value.emptyMap();
        expandedCmd.put("floats", floats);
        dataComponents.put("minecraft:custom_model_data", expandedCmd);

        return null;
    }
}
