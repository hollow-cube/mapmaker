package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V4657 extends DataVersion {
    public V4657() {
        super(4657);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
            "minecraft:creaking_heart",
            V4657::fixCreakingHeartBlockCreakingToActive
        ));
    }

    private static void fixCreakingHeartBlockCreakingToActive(Value blockProperties) {
        var creaking = blockProperties.remove("creaking").as(String.class, "dormant");
        if ("disabled".equalsIgnoreCase(creaking)) {
            blockProperties.put("neutral", "true");
        }
        blockProperties.put("creaking_heart_state", "active".equals(creaking) ? "awake" : "uprooted");
    }


}
