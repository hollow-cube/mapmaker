package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;

import java.util.Map;

public class V2700 extends DataVersion {
    private static final Map<String, String> RENAMES = Map.of(
            "minecraft:cave_vines_head", "minecraft:cave_vines",
            "minecraft:cave_vines_body", "minecraft:cave_vines_plant"
    );

    public V2700() {
        super(2700);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
    }
}
