package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2690 extends DataVersion {
    private static final Map<String, String> RENAMES;

    public V2690() {
        super(2690);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMES));
    }

    static {
        RENAMES = Map.ofEntries(
            Map.entry("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block"),
            Map.entry("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block"),
            Map.entry("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block"),
            Map.entry("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper"),
            Map.entry("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper"),
            Map.entry("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper"),
            Map.entry("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs"),
            Map.entry("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs"),
            Map.entry("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs"),
            Map.entry("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab"),
            Map.entry("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab"),
            Map.entry("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab"),
            Map.entry("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper"),
            Map.entry("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper"),
            Map.entry("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper"),
            Map.entry("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper"),
            Map.entry("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs"),
            Map.entry("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs"),
            Map.entry("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab"),
            Map.entry("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab")
        );
    }
}
