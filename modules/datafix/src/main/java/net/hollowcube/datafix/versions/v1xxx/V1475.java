package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;

import java.util.Map;

public class V1475 extends DataVersion {
    public V1475() {
        super(1475);

        var blockFix = new BlockRenameFix(Map.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"));
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
    }
}
