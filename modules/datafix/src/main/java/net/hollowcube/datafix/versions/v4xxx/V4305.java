package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V4305 extends DataVersion {

    public V4305() {
        super(4305);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
                "minecraft:test_block", V4305::fixTestBlockMode));
    }

    private static void fixTestBlockMode(Value blockProperties) {
        blockProperties.put("mode", blockProperties.remove("test_block_mode"));
    }

}
