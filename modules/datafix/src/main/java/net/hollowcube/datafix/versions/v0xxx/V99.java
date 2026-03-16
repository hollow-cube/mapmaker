package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V99 extends DataVersion {

    public V99() {
        super(99);

        registerEntities();
        registerBlockEntities();
        addReference(DataTypes.ENTITY, field -> field
            .extend(DataTypes.ENTITY_EQUIPMENT)
            .single("Riding", DataTypes.ENTITY));
        addReference(DataTypes.ITEM_STACK, field -> field
            .single("id", DataTypes.ITEM_NAME)
            .single("tag.EntityTag", DataTypes.ENTITY)
            .single("tag.BlockEntityTag", DataTypes.BLOCK_ENTITY)
            .list("tag.CanDestroy", DataTypes.BLOCK_NAME)
            .list("tag.CanPlaceOn", DataTypes.BLOCK_NAME)
            .list("tag.Items", DataTypes.ITEM_STACK)
            .list("tag.ChargedProjectiles", DataTypes.ITEM_STACK)
            .list("tag.pages", DataTypes.TEXT_COMPONENT)
            .list("tag.filtered_pages", DataTypes.TEXT_COMPONENT)
            .single("tag.display.Name", DataTypes.TEXT_COMPONENT)
            .list("tag.display.Lore", DataTypes.TEXT_COMPONENT));

        // TODO: The paper itemstack structure walker updates a lot of fields here, need to investigate more.
        //  it appears to basically be doing ADD_NAMES in mojangs version

        // TODO i am missing the concept of hooks.
        //  Hooks are fixes which apply to every single version in the future
        //  in this case, we need to add 3 hooks which call `namespaced`: BLOCK_NAME, ITEM_NAME, ITEM_STACK.
        //  questions:
        //  - do they need to apply to versions we would not otherwise do any changes? i assume no.
    }

    private void registerEntities() {
        addReference(DataTypes.ENTITY, "Item", field -> field.single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "XPOrb");
        addReference(DataTypes.ENTITY, "ThrownEgg", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "LeashKnot");
        addReference(DataTypes.ENTITY, "Painting");
        addReference(DataTypes.ENTITY, "Arrow", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "TippedArrow", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "SpectralArrow", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "Snowball", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "Fireball", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "SmallFireball", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "ThrownEnderpearl", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "EyeOfEnderSignal");
        addReference(DataTypes.ENTITY, "ThrownPotion", field -> V99.throwableProjectile(field)
            .single("Potion", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "ThrownExpBottle", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "ItemFrame", field -> field.single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "WitherSkull", V99::throwableProjectile);
        addReference(DataTypes.ENTITY, "PrimedTnt");
        addReference(DataTypes.ENTITY, "FallingSand", field -> field
            .single("Block", DataTypes.BLOCK_NAME)
            .single("TileEntityData", DataTypes.BLOCK_ENTITY));
        addReference(DataTypes.ENTITY, "FireworksRocketEntity", field -> field
            .single("FireworksItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "Minecart", field -> V99.minecart(field)
            .single("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "MinecartRideable", V99::minecart);
        addReference(DataTypes.ENTITY, "MinecartChest", field -> V99.minecart(field)
            .single("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "MinecartFurnace", V99::minecart);
        addReference(DataTypes.ENTITY, "MinecartTNT", V99::minecart);
        addReference(DataTypes.ENTITY, "MinecartSpawner", V99::minecart);
        addReference(DataTypes.ENTITY, "MinecartHopper", field -> V99.minecart(field)
            .single("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "MinecartCommandBlock", field -> V99.minecart(field)
            .single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.ENTITY, "ArmorStand");
        addReference(DataTypes.ENTITY, "Creeper");
        addReference(DataTypes.ENTITY, "Skeleton");
        addReference(DataTypes.ENTITY, "Spider");
        addReference(DataTypes.ENTITY, "Giant");
        addReference(DataTypes.ENTITY, "Zombie");
        addReference(DataTypes.ENTITY, "Slime");
        addReference(DataTypes.ENTITY, "Ghast");
        addReference(DataTypes.ENTITY, "PigZombie");
        addReference(DataTypes.ENTITY, "Enderman", field -> field.single("carried", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "CaveSpider");
        addReference(DataTypes.ENTITY, "Silverfish");
        addReference(DataTypes.ENTITY, "Blaze");
        addReference(DataTypes.ENTITY, "LavaSlime");
        addReference(DataTypes.ENTITY, "EnderDragon");
        addReference(DataTypes.ENTITY, "WitherBoss");
        addReference(DataTypes.ENTITY, "Bat");
        addReference(DataTypes.ENTITY, "Witch");
        addReference(DataTypes.ENTITY, "Endermite");
        addReference(DataTypes.ENTITY, "Guardian");
        addReference(DataTypes.ENTITY, "Pig");
        addReference(DataTypes.ENTITY, "Sheep");
        addReference(DataTypes.ENTITY, "Cow");
        addReference(DataTypes.ENTITY, "Chicken");
        addReference(DataTypes.ENTITY, "Squid");
        addReference(DataTypes.ENTITY, "Wolf");
        addReference(DataTypes.ENTITY, "MushroomCow");
        addReference(DataTypes.ENTITY, "SnowMan");
        addReference(DataTypes.ENTITY, "Ozelot");
        addReference(DataTypes.ENTITY, "VillagerGolem");
        addReference(DataTypes.ENTITY, "EntityHorse", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("ArmorItem", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "Rabbit");
        addReference(DataTypes.ENTITY, "Villager", field -> field.list("Inventory", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "EnderCrystal");
        addReference(DataTypes.ENTITY, "AreaEffectCloud");
        addReference(DataTypes.ENTITY, "ShulkerBullet");
        addReference(DataTypes.ENTITY, "DragonFireball");
        addReference(DataTypes.ENTITY, "Shulker");
    }

    private static DataType.Builder throwableProjectile(DataType.Builder field) {
        return field.single("inTile", DataTypes.BLOCK_NAME);
    }

    private static DataType.Builder minecart(DataType.Builder field) {
        return field.single("DisplayTile", DataTypes.BLOCK_NAME);
    }

    private void registerBlockEntities() {
        addReference(DataTypes.BLOCK_ENTITY, field -> field.single("components", DataTypes.DATA_COMPONENTS));
        addReference(DataTypes.BLOCK_ENTITY, "Furnace", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "Chest", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "EnderChest");
        addReference(DataTypes.BLOCK_ENTITY, "RecordPlayer", field -> field.single("RecordItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.BLOCK_ENTITY, "Trap", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "Dropper", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "Sign", V99::signBlock);
        addReference(DataTypes.BLOCK_ENTITY, "MobSpawner");
        addReference(DataTypes.BLOCK_ENTITY, "Music");
        addReference(DataTypes.BLOCK_ENTITY, "Piston");
        addReference(DataTypes.BLOCK_ENTITY, "Cauldron", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "EnchantTable");
        addReference(DataTypes.BLOCK_ENTITY, "Airportal");
        addReference(DataTypes.BLOCK_ENTITY, "Control", field -> field.single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "Beacon");
        addReference(DataTypes.BLOCK_ENTITY, "Skull", field -> field.single("custom_name", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "DLDetector");
        addReference(DataTypes.BLOCK_ENTITY, "Hopper", V99::inventoryBlock);
        addReference(DataTypes.BLOCK_ENTITY, "Comparator");
        addReference(DataTypes.BLOCK_ENTITY, "FlowerPot", field -> field.single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.BLOCK_ENTITY, "Structure");
        addReference(DataTypes.BLOCK_ENTITY, "EndGateway");
    }

    private static DataType.Builder inventoryBlock(DataType.Builder field) {
        return field.list("Items", DataTypes.ITEM_STACK);
    }

    public static DataType.Builder signBlock(DataType.Builder field) {
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
