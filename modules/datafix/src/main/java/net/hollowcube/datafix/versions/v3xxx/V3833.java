package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3833 extends DataVersion {
    public V3833() {
        super(3833);

        addFix(DataType.BLOCK_ENTITY, "minecraft:brushable_block", V3833::fixRemoveEmptyItemInBrushableBlock);
    }

    private static Value fixRemoveEmptyItemInBrushableBlock(Value blockEntity) {
        var item = blockEntity.get("item");
        if (item.isNull()) return null;

        var itemId = item.get("id").as(String.class, "minecraft:air");
        var count = item.get("count").as(Number.class, 0).intValue();
        if ("minecraft:air".equals(itemId) && count == 0)
            blockEntity.remove("item");

        return null;
    }
}
