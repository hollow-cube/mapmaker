package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V4300 extends DataVersion {
    public V4300() {
        super(4300);

        addReference(DataType.ENTITY, "minecraft:llama", V4300::entityWithInventory);
        addReference(DataType.ENTITY, "minecraft:trader_llama", V4300::entityWithInventory);
        addReference(DataType.ENTITY, "minecraft:donkey", V4300::entityWithInventory);
        addReference(DataType.ENTITY, "minecraft:mule", V4300::entityWithInventory);
        addReference(DataType.ENTITY, "minecraft:horse");
        addReference(DataType.ENTITY, "minecraft:skeleton_horse");
        addReference(DataType.ENTITY, "minecraft:zombie_horse");
    }

    static @NotNull Field entityWithInventory(@NotNull Field field) {
        return field
                .list("Items", DataType.ITEM_STACK);
    }

}
