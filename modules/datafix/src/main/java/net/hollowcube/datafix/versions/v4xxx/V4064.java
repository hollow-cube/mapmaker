package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.DataComponentRename;
import net.hollowcube.datafix.util.Value;

public class V4064 extends DataVersion {
    public V4064() {
        super(4064);

        addFix(DataTypes.DATA_COMPONENTS, new DataComponentRename(
                "minecraft:fire_resistant", "minecraft:damage_resistant",
                ignored -> {
                    var newValue = Value.emptyMap();
                    newValue.put("types", "#minecraft:is_fire");
                    return newValue;
                }
        ));
    }
}
