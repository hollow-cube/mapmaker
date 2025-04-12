package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V99 extends DataVersion {

    public V99() {
        super(99);

        registerEntities();
        registerBlockEntities();
        addReference(DataType.ENTITY_TREE, field -> field.extend(DataType.ENTITY).single("Riding", DataType.ENTITY));
        addReference(DataType.ENTITY, field -> field.extend(DataType.ENTITY_EQUIPMENT));
        addReference(DataType.ITEM_STACK, field -> field.single("id", DataType.ITEM_NAME)
                .single("EntityTag", DataType.ENTITY_TREE)
                .single("BlockEntityTag", DataType.BLOCK_ENTITY)
                .list("CanDestroy", DataType.BLOCK_NAME)
                .list("CanPlaceOn", DataType.BLOCK_NAME)
                .list("Items", DataType.ITEM_STACK)
                .list("ChargedProjectiles", DataType.ITEM_STACK)
                .list("pages", DataType.TEXT_COMPONENT)
                .list("filtered_pages", DataType.TEXT_COMPONENT)
                .single("display.Name", DataType.TEXT_COMPONENT)
                .list("display.Lore", DataType.TEXT_COMPONENT));
    }

    private void registerEntities() {
        addReference(DataType.ENTITY, "Item", field -> field.single("Item", DataType.ITEM_STACK));
        // blah blah
        addReference(DataType.ENTITY, "Villager", field -> field.list("Inventory", DataType.ITEM_STACK));
    }

    private void registerBlockEntities() {
        addReference(DataType.BLOCK_ENTITY, field -> field.single("components", DataType.DATA_COMPONENTS)); // todo added way later?
        addInventoryBlock("Furnace");
        addInventoryBlock("Chest");
        addReference(DataType.BLOCK_ENTITY, "EnderChest");
        addReference(DataType.BLOCK_ENTITY, "RecordPlayer", field -> field.single("RecordItem", DataType.ITEM_STACK));
        addInventoryBlock("Trap");
        addInventoryBlock("Dropper");
        addReference(DataType.BLOCK_ENTITY, "Sign", V99::signBlock);
        addReference(DataType.BLOCK_ENTITY, "MobSpawner");
        addReference(DataType.BLOCK_ENTITY, "Music");
        addReference(DataType.BLOCK_ENTITY, "Piston");
        addInventoryBlock("Cauldron");
        addReference(DataType.BLOCK_ENTITY, "EnchantTable");
        addReference(DataType.BLOCK_ENTITY, "Airportal");
        addReference(DataType.BLOCK_ENTITY, "Control", field -> field.single("LastOutput", DataType.TEXT_COMPONENT));
        addReference(DataType.BLOCK_ENTITY, "Beacon");
        addReference(DataType.BLOCK_ENTITY, "Skull", field -> field.single("custom_name", DataType.TEXT_COMPONENT));
        addReference(DataType.BLOCK_ENTITY, "DLDetector");
        addInventoryBlock("Hopper");
        addReference(DataType.BLOCK_ENTITY, "Comparator");
        addReference(DataType.BLOCK_ENTITY, "FlowerPot", field -> field.single("Item", DataType.ITEM_STACK)); // TODO item can also be an int.
        addReference(DataType.BLOCK_ENTITY, "Structure");
        addReference(DataType.BLOCK_ENTITY, "EndGateway");
    }

    private void addInventoryBlock(@NotNull String id) {
        addReference(DataType.BLOCK_ENTITY, id, field -> field.list("Items", DataType.ITEM_STACK));
    }

    static @NotNull Field signBlock(@NotNull Field field) {
        return field
                .single("Text1", DataType.TEXT_COMPONENT)
                .single("Text2", DataType.TEXT_COMPONENT)
                .single("Text3", DataType.TEXT_COMPONENT)
                .single("Text4", DataType.TEXT_COMPONENT)
                .single("FilteredText1", DataType.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText2", DataType.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText3", DataType.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText4", DataType.TEXT_COMPONENT); // TODO: not added until later
    }

    // TODO: What does V99.addNames accomplish?
}
