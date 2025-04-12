package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V1490 extends DataVersion {
    private static final Map<String, String> RENAMED_ITEM_IDS = Map.of(
            "minecraft:melon_block", "minecraft:melon",
            "minecraft:melon", "minecraft:melon_slice",
            "minecraft:speckled_melon", "minecraft:glistering_melon_slice"
    );

    public V1490() {
        super(1490);

        var blockFix = new BlockRenameFix("minecraft:melon_block", "minecraft:melon");
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMED_ITEM_IDS));
    }
}
