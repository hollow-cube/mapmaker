package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V1487 extends DataVersion {
    private static final Map<String, String> RENAMED_IDS = Map.of(
            "minecraft:prismarine_bricks_slab", "minecraft:prismarine_brick_slab",
            "minecraft:prismarine_bricks_stairs", "minecraft:prismarine_brick_stairs"
    );

    public V1487() {
        super(1487);

        var blockFix = new BlockRenameFix(RENAMED_IDS);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMED_IDS));
    }
}
