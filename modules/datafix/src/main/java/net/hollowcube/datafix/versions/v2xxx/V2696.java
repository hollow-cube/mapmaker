package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2696 extends DataVersion {
    private static final Map<String, String> RENAMES;

    public V2696() {
        super(2696);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMES));
    }

    static {
        RENAMES = Map.ofEntries(
            Map.entry("minecraft:grimstone", "minecraft:deepslate"),
            Map.entry("minecraft:grimstone_slab", "minecraft:cobbled_deepslate_slab"),
            Map.entry("minecraft:grimstone_stairs", "minecraft:cobbled_deepslate_stairs"),
            Map.entry("minecraft:grimstone_wall", "minecraft:cobbled_deepslate_wall"),
            Map.entry("minecraft:polished_grimstone", "minecraft:polished_deepslate"),
            Map.entry("minecraft:polished_grimstone_slab", "minecraft:polished_deepslate_slab"),
            Map.entry("minecraft:polished_grimstone_stairs", "minecraft:polished_deepslate_stairs"),
            Map.entry("minecraft:polished_grimstone_wall", "minecraft:polished_deepslate_wall"),
            Map.entry("minecraft:grimstone_tiles", "minecraft:deepslate_tiles"),
            Map.entry("minecraft:grimstone_tile_slab", "minecraft:deepslate_tile_slab"),
            Map.entry("minecraft:grimstone_tile_stairs", "minecraft:deepslate_tile_stairs"),
            Map.entry("minecraft:grimstone_tile_wall", "minecraft:deepslate_tile_wall"),
            Map.entry("minecraft:grimstone_bricks", "minecraft:deepslate_bricks"),
            Map.entry("minecraft:grimstone_brick_slab", "minecraft:deepslate_brick_slab"),
            Map.entry("minecraft:grimstone_brick_stairs", "minecraft:deepslate_brick_stairs"),
            Map.entry("minecraft:grimstone_brick_wall", "minecraft:deepslate_brick_wall"),
            Map.entry("minecraft:chiseled_grimstone", "minecraft:chiseled_deepslate")
        );
    }
}
