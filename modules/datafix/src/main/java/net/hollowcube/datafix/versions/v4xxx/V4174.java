package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V4174 extends DataVersion {
    public V4174() {
        super(4174);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
                "minecraft:creaking_heart",
                V4174::fixCreakingHeartBlockCreakingToActive
        ));
    }

    private static void fixCreakingHeartBlockCreakingToActive(Value blockProperties) {
        var creaking = blockProperties.remove("creaking").as(String.class, "dormant");
        if ("disabled".equalsIgnoreCase(creaking)) {
            blockProperties.put("neutral", "true");
        }
        blockProperties.put("active", "active".equals(creaking) ? "true" : "false");
    }

}