package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3809 extends DataVersion {
    public V3809() {
        super(3809);

        addFix(DataType.ENTITY, "minecraft:llama", V3809::fixHorseLikeInventoryIndexing);
        addFix(DataType.ENTITY, "minecraft:trader_llama", V3809::fixHorseLikeInventoryIndexing);
        addFix(DataType.ENTITY, "minecraft:mule", V3809::fixHorseLikeInventoryIndexing);
        addFix(DataType.ENTITY, "minecraft:donkey", V3809::fixHorseLikeInventoryIndexing);
    }

    private static Value fixHorseLikeInventoryIndexing(Value value) {
        for (var itemStack : value.get("Items")) {
            itemStack.put("Slot", (byte) (itemStack.get("Slot")
                    .as(Number.class, 2).byteValue() - 2));
        }
        return null;
    }
}
