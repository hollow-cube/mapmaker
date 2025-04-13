package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class V1458 extends DataVersion {
    private static final Set<String> NAMEABLE_BLOCK_ENTITIES;

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

        addFix(DataType.ENTITY, V1458::fixEntityCustomName);
        addFix(DataType.ITEM_STACK, V1458::fixItemCustomName);
        addFix(DataType.BLOCK_ENTITY, V1458::fixBlockEntityCustomName);
    }

    static @NotNull Field nameableInventory(@NotNull Field field) {
        return field
                .list("Items", DataType.ITEM_STACK)
                .single("CustomName", DataType.TEXT_COMPONENT);
    }

    static @NotNull Field nameable(@NotNull Field field) {
        return field.single("CustomName", DataType.TEXT_COMPONENT);
    }

    private static Value fixEntityCustomName(Value value) {
        if ("minecraft:commandblock_minecart".equals(value.getValue("id")))
            return null;
        // TODO: we should probably use adventure legacy parser here?
        String customName = value.get("CustomName").as(String.class, "");
        value.put("CustomName", "{\"text\":\"" + customName + "\"}");
        return null;
    }

    private static Value fixItemCustomName(Value value) {
        var display = value.get("tag").get("display");
        var customNameValue = display.get("Name");
        if (customNameValue.value() == null) return null;

        String customName = customNameValue.as(String.class, "");
        display.put("Name", "{\"text\":\"" + customName + "\"}");
        return null;
    }

    private static Value fixBlockEntityCustomName(Value value) {
        String id = value.get("id").as(String.class, "");
        if (!id.isEmpty() && !NAMEABLE_BLOCK_ENTITIES.contains(id))
            return null;

        var customName = value.get("CustomName").as(String.class, "");
        value.put("CustomName", customName.isEmpty() ? null : "{\"text\":\"" + customName + "\"}");
        return null;
    }

    static {
        NAMEABLE_BLOCK_ENTITIES = Set.of(
                "minecraft:beacon",
                "minecraft:banner",
                "minecraft:brewing_stand",
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:dispenser",
                "minecraft:dropper",
                "minecraft:enchanting_table",
                "minecraft:furnace",
                "minecraft:hopper",
                "minecraft:shulker_box"
        );
    }
}
