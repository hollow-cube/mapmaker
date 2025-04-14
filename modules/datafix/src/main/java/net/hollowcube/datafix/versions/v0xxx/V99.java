package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V99 extends DataVersion {

    public V99() {
        super(99);

        registerEntities();
        registerBlockEntities();
        addReference(DataTypes.ENTITY_TREE, field -> field.extend(DataTypes.ENTITY).single("Riding", DataTypes.ENTITY));
        addReference(DataTypes.ENTITY, field -> field.extend(DataTypes.ENTITY_EQUIPMENT));
        addReference(DataTypes.ITEM_STACK, field -> field.single("id", DataTypes.ITEM_NAME)
                .single("EntityTag", DataTypes.ENTITY_TREE)
                .single("BlockEntityTag", DataTypes.BLOCK_ENTITY)
                .list("CanDestroy", DataTypes.BLOCK_NAME)
                .list("CanPlaceOn", DataTypes.BLOCK_NAME)
                .list("Items", DataTypes.ITEM_STACK)
                .list("ChargedProjectiles", DataTypes.ITEM_STACK)
                .list("pages", DataTypes.TEXT_COMPONENT)
                .list("filtered_pages", DataTypes.TEXT_COMPONENT)
                .single("display.Name", DataTypes.TEXT_COMPONENT)
                .list("display.Lore", DataTypes.TEXT_COMPONENT));
    }

    private void registerEntities() {
        addReference(DataTypes.ENTITY, "Item", field -> field.single("Item", DataTypes.ITEM_STACK));
        // blah blah
        addReference(DataTypes.ENTITY, "Villager", field -> field.list("Inventory", DataTypes.ITEM_STACK));
    }

    private void registerBlockEntities() {
        addReference(DataTypes.BLOCK_ENTITY, field -> field.single("components", DataTypes.DATA_COMPONENTS)); // todo added way later?
        addInventoryBlock("Furnace");
        addInventoryBlock("Chest");
        addReference(DataTypes.BLOCK_ENTITY, "EnderChest");
        addReference(DataTypes.BLOCK_ENTITY, "RecordPlayer", field -> field.single("RecordItem", DataTypes.ITEM_STACK));
        addInventoryBlock("Trap");
        addInventoryBlock("Dropper");
        addReference(DataTypes.BLOCK_ENTITY, "Sign", V99::signBlock);
        addReference(DataTypes.BLOCK_ENTITY, "MobSpawner");
        addReference(DataTypes.BLOCK_ENTITY, "Music");
        addReference(DataTypes.BLOCK_ENTITY, "Piston");
        addInventoryBlock("Cauldron");
        addReference(DataTypes.BLOCK_ENTITY, "EnchantTable");
        addReference(DataTypes.BLOCK_ENTITY, "Airportal");
        addReference(DataTypes.BLOCK_ENTITY, "Control", field -> field.single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "Beacon");
        addReference(DataTypes.BLOCK_ENTITY, "Skull", field -> field.single("custom_name", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "DLDetector");
        addInventoryBlock("Hopper");
        addReference(DataTypes.BLOCK_ENTITY, "Comparator");
        addReference(DataTypes.BLOCK_ENTITY, "FlowerPot", field -> field.single("Item", DataTypes.ITEM_STACK)); // TODO item can also be an int.
        addReference(DataTypes.BLOCK_ENTITY, "Structure");
        addReference(DataTypes.BLOCK_ENTITY, "EndGateway");
    }

    private void addInventoryBlock(@NotNull String id) {
        addReference(DataTypes.BLOCK_ENTITY, id, field -> field.list("Items", DataTypes.ITEM_STACK));
    }

    public static @NotNull DataType.Builder signBlock(@NotNull DataType.Builder field) {
        return field
                .single("Text1", DataTypes.TEXT_COMPONENT)
                .single("Text2", DataTypes.TEXT_COMPONENT)
                .single("Text3", DataTypes.TEXT_COMPONENT)
                .single("Text4", DataTypes.TEXT_COMPONENT)
                .single("FilteredText1", DataTypes.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText2", DataTypes.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText3", DataTypes.TEXT_COMPONENT) // TODO: not added until later
                .single("FilteredText4", DataTypes.TEXT_COMPONENT); // TODO: not added until later
    }

    // TODO: What does V99.addNames accomplish?
}
