package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;

import java.util.Map;

public class V1475 extends DataVersion {
    public V1475() {
        super(1475);

        var blockFix = new BlockRenameFix(Map.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"));
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
    }
}
