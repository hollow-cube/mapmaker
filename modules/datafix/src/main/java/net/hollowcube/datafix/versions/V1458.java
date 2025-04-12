package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V1458 extends DataVersion {
    public V1458() {
        super(1458);

        addReference(DataType.ENTITY, V1458::nameable);

        addReference(DataType.BLOCK_ENTITY, "minecraft:beacon", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:banner", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:brewing_stand", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:chest", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:trapped_chest", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:dispenser", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:dropper", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:enchanting_table", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:furnace", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:hopper", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:shulker_box", V1458::nameableInventory);
    }

    static @NotNull Field nameableInventory(@NotNull Field field) {
        return field
                .list("Items", DataType.ITEM_STACK)
                .single("CustomName", DataType.TEXT_COMPONENT);
    }

    static @NotNull Field nameable(@NotNull Field field) {
        return field.single("CustomName", DataType.TEXT_COMPONENT);
    }
}
