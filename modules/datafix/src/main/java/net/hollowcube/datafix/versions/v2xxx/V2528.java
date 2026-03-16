package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2528 extends DataVersion {
    private static final Map<String, String> BLOCK_RENAMES = Map.of(
        "minecraft:soul_fire_torch", "minecraft:soul_torch",
        "minecraft:soul_fire_wall_torch", "minecraft:soul_wall_torch",
        "minecraft:soul_fire_lantern", "minecraft:soul_lantern"
    );
    private static final Map<String, String> ITEM_RENAMES = Map.of(
        "minecraft:soul_fire_torch", "minecraft:soul_torch",
        "minecraft:soul_fire_lantern", "minecraft:soul_lantern"
    );

    public V2528() {
        super(2528);

        var blockFix = new BlockRenameFix(BLOCK_RENAMES);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(ITEM_RENAMES));
    }
}
