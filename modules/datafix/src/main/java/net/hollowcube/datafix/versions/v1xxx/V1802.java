package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;

import java.util.Map;

public class V1802 extends DataVersion {
    private static final Map<String, String> RENAMED_BLOCKS = Map.of(
            "minecraft:stone_slab", "minecraft:smooth_stone_slab",
            "minecraft:sign", "minecraft:oak_sign",
            "minecraft:wall_sign", "minecraft:oak_wall_sign");
    private static final Map<String, String> RENAMED_ITEMS = Map.of(
            "minecraft:stone_slab", "minecraft:smooth_stone_slab",
            "minecraft:sign", "minecraft:oak_sign");

    public V1802() {
        super(1802);

        var blockFix = new BlockRenameFix(RENAMED_BLOCKS);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new BlockRenameFix(RENAMED_ITEMS));
    }
}
