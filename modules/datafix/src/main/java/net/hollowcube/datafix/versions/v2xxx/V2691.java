package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2691 extends DataVersion {
    private static final Map<String, String> RENAMES = Map.of(
            "minecraft:waxed_copper", "minecraft:waxed_copper_block",
            "minecraft:oxidized_copper_block", "minecraft:oxidized_copper",
            "minecraft:weathered_copper_block", "minecraft:weathered_copper",
            "minecraft:exposed_copper_block", "minecraft:exposed_copper"
    );

    public V2691() {
        super(2691);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMES));
    }
}
