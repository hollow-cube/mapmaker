package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V5012 extends DataVersion {
    private static final Map<String, String> RENAMES = Map.of(
            "minecraft:ocean_explorer_map", "minecraft:ocean_monument_map",
            "minecraft:swamp_explorer_map", "minecraft:swamp_hut_map",
            "minecraft:trial_explorer_map", "minecraft:buried_trial_chambers_map",
            "minecraft:woodland_explorer_map", "minecraft:woodland_mansion_map",
            "minecraft:jungle_explorer_map", "minecraft:jungle_pyramid_map"
    );

    public V5012() {
        super(5012);

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMES));
    }

}
