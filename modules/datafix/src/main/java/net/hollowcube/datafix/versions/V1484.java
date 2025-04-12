package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V1484 extends DataVersion {
    private static final Map<String, String> SEAGRASS_IDS = Map.of(
            "minecraft:sea_grass", "minecraft:seagrass",
            "minecraft:tall_sea_grass", "minecraft:tall_seagrass"
    );

    public V1484() {
        super(1484);

        var blockFix = new BlockRenameFix(SEAGRASS_IDS);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(SEAGRASS_IDS));
    }
}
