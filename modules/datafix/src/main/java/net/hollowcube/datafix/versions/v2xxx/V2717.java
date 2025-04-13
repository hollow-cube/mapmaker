package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2717 extends DataVersion {
    private static final Map<String, String> RENAMES = Map.of(
            "minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"
    );

    public V2717() {
        super(2717);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMES));
    }
}
