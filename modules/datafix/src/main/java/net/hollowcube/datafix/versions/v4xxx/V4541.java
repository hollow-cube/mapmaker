package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.util.Value;

public class V4541 extends DataVersion {

    public V4541() {
        super(4541);

        var blockFix = new BlockRenameFix("minecraft:chain", "minecraft:iron_chain");
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix("minecraft:chain", "minecraft:iron_chain"));
    }

}
