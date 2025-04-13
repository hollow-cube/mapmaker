package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V2680 extends DataVersion {
    private static final Map<String, String> RENAMES = Map.of(
            "minecraft:grass_path", "minecraft:dirt_path"
    );

    public V2680() {
        super(2680);

        var blockFix = new BlockRenameFix(RENAMES);
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(RENAMES));
    }
}
