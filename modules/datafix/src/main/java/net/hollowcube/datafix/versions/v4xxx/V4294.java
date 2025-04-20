package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V4294 extends DataVersion {

    public V4294() {
        super(4294);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
                "minecraft:creaking_heart", V4294::fixCreakingHeartBlockActiveToState));
    }

    private static void fixCreakingHeartBlockActiveToState(Value blockProperties) {
        var active = blockProperties.remove("active").as(String.class, "false");
        blockProperties.put("creaking_heart_state", "true".equals(active) ? "awake" : "uprooted");
    }

}
