package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2508 extends DataVersion {
    private static final Map<String, String> RENAMED_IDS = Map.of(
        "minecraft:warped_fungi", "minecraft:warped_fungus",
        "minecraft:crimson_fungi", "minecraft:crimson_fungus"
    );

    public V2508() {
        super(2508);

        var blockFix = new BlockRenameFix(RENAMED_IDS);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMED_IDS));
    }
}
