package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.util.Value;

public class V1474 extends DataVersion {
    public V1474() {
        super(1474);

        addFix(DataType.ENTITY, "minecraft:shulker", V1474::fixColorlessShulkerEntity);
        var blockFix = new BlockRenameFix("minecraft:purple_shulker_box", "minecraft:shulker_box");
        addFix(DataType.BLOCK_NAME, blockFix);
        addFix(DataType.BLOCK_STATE, blockFix);
        addFix(DataType.FLAT_BLOCK_STATE, blockFix);
        addFix(DataType.ITEM_NAME, new ItemRenameFix("minecraft:purple_shulker_box", "minecraft:shulker_box"));
    }

    private static Value fixColorlessShulkerEntity(Value value) {
        int color = value.get("Color").as(Number.class, 0).intValue();
        if (color == 10) value.put("Color", (byte) 16);
        return null;
    }
}
