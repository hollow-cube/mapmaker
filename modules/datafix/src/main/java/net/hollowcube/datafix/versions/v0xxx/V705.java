package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V705 extends DataVersion {
    private static final Map<String, String> ENTITY_ID_MAP;

    public V705() {
        super(705);

        addReference(DataTypes.ENTITY, "minecraft:area_effect_cloud");
        addReference(DataTypes.ENTITY, "minecraft:armor_stand");
        addReference(DataTypes.ENTITY, "minecraft:arrow", field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:bat");
        addReference(DataTypes.ENTITY, "minecraft:blaze");
        addReference(DataTypes.ENTITY, "minecraft:boat");
        addReference(DataTypes.ENTITY, "minecraft:cave_spider");
        addReference(DataTypes.ENTITY, "minecraft:chest_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME)
            .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:chicken");
        addReference(DataTypes.ENTITY, "minecraft:commandblock_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME)
            .single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.ENTITY, "minecraft:cow");
        addReference(DataTypes.ENTITY, "minecraft:creeper");
        addReference(DataTypes.ENTITY, "minecraft:donkey", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:dragon_fireball");
        addProjectileEntity("minecraft:egg");
        addReference(DataTypes.ENTITY, "minecraft:elder_guardian");
        addReference(DataTypes.ENTITY, "minecraft:ender_crystal");
        addReference(DataTypes.ENTITY, "minecraft:ender_dragon");
        addReference(DataTypes.ENTITY, "minecraft:enderman", field -> field
            .single("carried", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:endermite");
        addProjectileEntity("minecraft:ender_pearl");
        addReference(DataTypes.ENTITY, "minecraft:eye_of_ender_signal");
        addReference(DataTypes.ENTITY, "minecraft:falling_block", field -> field
            .single("Block", DataTypes.BLOCK_NAME)
            .single("TileEntityData", DataTypes.BLOCK_ENTITY));
        addProjectileEntity("minecraft:fireball");
        addReference(DataTypes.ENTITY, "minecraft:fireworks_rocket", field -> field
            .single("FireworksItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:furnace_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:ghast");
        addReference(DataTypes.ENTITY, "minecraft:giant");
        addReference(DataTypes.ENTITY, "minecraft:guardian");
        addReference(DataTypes.ENTITY, "minecraft:hopper_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME)
            .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:horse", field -> field
            .single("ArmorItem", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:husk");
        addReference(DataTypes.ENTITY, "minecraft:item", field -> field
            .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:item_frame", field -> field
            .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:leash_knot");
        addReference(DataTypes.ENTITY, "minecraft:magma_cube");
        addReference(DataTypes.ENTITY, "minecraft:minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:mooshroom");
        addReference(DataTypes.ENTITY, "minecraft:mule", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:ocelot");
        addReference(DataTypes.ENTITY, "minecraft:painting");
        addReference(DataTypes.ENTITY, "minecraft:parrot");
        addReference(DataTypes.ENTITY, "minecraft:pig");
        addReference(DataTypes.ENTITY, "minecraft:polar_bear");
        addReference(DataTypes.ENTITY, "minecraft:potion", field -> field
            .single("Potion", DataTypes.ITEM_STACK)
            .single("inTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:rabbit");
        addReference(DataTypes.ENTITY, "minecraft:sheep");
        addReference(DataTypes.ENTITY, "minecraft:shulker");
        addReference(DataTypes.ENTITY, "minecraft:shulker_bullet");
        addReference(DataTypes.ENTITY, "minecraft:silverfish");
        addReference(DataTypes.ENTITY, "minecraft:skeleton");
        addReference(DataTypes.ENTITY, "minecraft:skeleton_horse", field -> field
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:slime");
        addProjectileEntity("minecraft:small_fireball");
        addProjectileEntity("minecraft:snowball");
        addReference(DataTypes.ENTITY, "minecraft:snowman");
        addReference(DataTypes.ENTITY, "minecraft:spawner_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:spider");
        addReference(DataTypes.ENTITY, "minecraft:squid");
        addReference(DataTypes.ENTITY, "minecraft:stray");
        addReference(DataTypes.ENTITY, "minecraft:tnt");
        addReference(DataTypes.ENTITY, "minecraft:tnt_minecart", field -> field
            .single("DisplayTile", DataTypes.BLOCK_NAME));
        addReference(DataTypes.ENTITY, "minecraft:villager", field -> field
            .list("Inventory", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:villager_golem");
        addReference(DataTypes.ENTITY, "minecraft:witch");
        addReference(DataTypes.ENTITY, "minecraft:wither");
        addReference(DataTypes.ENTITY, "minecraft:wither_skeleton");
        addProjectileEntity("minecraft:wither_skull");
        addReference(DataTypes.ENTITY, "minecraft:wolf");
        addProjectileEntity("minecraft:xp_bottle");
        addReference(DataTypes.ENTITY, "minecraft:xp_orb");
        addReference(DataTypes.ENTITY, "minecraft:zombie");
        addReference(DataTypes.ENTITY, "minecraft:zombie_horse", field -> field
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:zombie_pigman");
        addReference(DataTypes.ENTITY, "minecraft:zombie_villager");
        addReference(DataTypes.ENTITY, "minecraft:evocation_fangs");
        addReference(DataTypes.ENTITY, "minecraft:evocation_illager");
        addReference(DataTypes.ENTITY, "minecraft:illusion_illager");
        addReference(DataTypes.ENTITY, "minecraft:llama", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK)
            .single("DecorItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:llama_spit");
        addReference(DataTypes.ENTITY, "minecraft:vex");
        addReference(DataTypes.ENTITY, "minecraft:vindication_illager");

        addFix(DataTypes.ENTITY, new EntityRenameFix(V705::fixEntityId));

        // TODO: Now we need structure hooks here to ensure entity IDs are namespaced as well.
    }

    private void addProjectileEntity(String id) {
        addReference(DataTypes.ENTITY, id, field -> field
            .single("inTile", DataTypes.BLOCK_NAME));
    }

    private static String fixEntityId(Value ignored, String id) {
        return ENTITY_ID_MAP.getOrDefault(id, id);
    }

    static {
        ENTITY_ID_MAP = Map.<String, String>ofEntries(
            Map.entry("AreaEffectCloud", "minecraft:area_effect_cloud"),
            Map.entry("ArmorStand", "minecraft:armor_stand"),
            Map.entry("Arrow", "minecraft:arrow"),
            Map.entry("Bat", "minecraft:bat"),
            Map.entry("Blaze", "minecraft:blaze"),
            Map.entry("Boat", "minecraft:boat"),
            Map.entry("CaveSpider", "minecraft:cave_spider"),
            Map.entry("Chicken", "minecraft:chicken"),
            Map.entry("Cow", "minecraft:cow"),
            Map.entry("Creeper", "minecraft:creeper"),
            Map.entry("Donkey", "minecraft:donkey"),
            Map.entry("DragonFireball", "minecraft:dragon_fireball"),
            Map.entry("ElderGuardian", "minecraft:elder_guardian"),
            Map.entry("EnderCrystal", "minecraft:ender_crystal"),
            Map.entry("EnderDragon", "minecraft:ender_dragon"),
            Map.entry("Enderman", "minecraft:enderman"),
            Map.entry("Endermite", "minecraft:endermite"),
            Map.entry("EyeOfEnderSignal", "minecraft:eye_of_ender_signal"),
            Map.entry("FallingSand", "minecraft:falling_block"),
            Map.entry("Fireball", "minecraft:fireball"),
            Map.entry("FireworksRocketEntity", "minecraft:fireworks_rocket"),
            Map.entry("Ghast", "minecraft:ghast"),
            Map.entry("Giant", "minecraft:giant"),
            Map.entry("Guardian", "minecraft:guardian"),
            Map.entry("Horse", "minecraft:horse"),
            Map.entry("Husk", "minecraft:husk"),
            Map.entry("Item", "minecraft:item"),
            Map.entry("ItemFrame", "minecraft:item_frame"),
            Map.entry("LavaSlime", "minecraft:magma_cube"),
            Map.entry("LeashKnot", "minecraft:leash_knot"),
            Map.entry("MinecartChest", "minecraft:chest_minecart"),
            Map.entry("MinecartCommandBlock", "minecraft:commandblock_minecart"),
            Map.entry("MinecartFurnace", "minecraft:furnace_minecart"),
            Map.entry("MinecartHopper", "minecraft:hopper_minecart"),
            Map.entry("MinecartRideable", "minecraft:minecart"),
            Map.entry("MinecartSpawner", "minecraft:spawner_minecart"),
            Map.entry("MinecartTNT", "minecraft:tnt_minecart"),
            Map.entry("Mule", "minecraft:mule"),
            Map.entry("MushroomCow", "minecraft:mooshroom"),
            Map.entry("Ozelot", "minecraft:ocelot"),
            Map.entry("Painting", "minecraft:painting"),
            Map.entry("Pig", "minecraft:pig"),
            Map.entry("PigZombie", "minecraft:zombie_pigman"),
            Map.entry("PolarBear", "minecraft:polar_bear"),
            Map.entry("PrimedTnt", "minecraft:tnt"),
            Map.entry("Rabbit", "minecraft:rabbit"),
            Map.entry("Sheep", "minecraft:sheep"),
            Map.entry("Shulker", "minecraft:shulker"),
            Map.entry("ShulkerBullet", "minecraft:shulker_bullet"),
            Map.entry("Silverfish", "minecraft:silverfish"),
            Map.entry("Skeleton", "minecraft:skeleton"),
            Map.entry("SkeletonHorse", "minecraft:skeleton_horse"),
            Map.entry("Slime", "minecraft:slime"),
            Map.entry("SmallFireball", "minecraft:small_fireball"),
            Map.entry("SnowMan", "minecraft:snowman"),
            Map.entry("Snowball", "minecraft:snowball"),
            Map.entry("SpectralArrow", "minecraft:spectral_arrow"),
            Map.entry("Spider", "minecraft:spider"),
            Map.entry("Squid", "minecraft:squid"),
            Map.entry("Stray", "minecraft:stray"),
            Map.entry("ThrownEgg", "minecraft:egg"),
            Map.entry("ThrownEnderpearl", "minecraft:ender_pearl"),
            Map.entry("ThrownExpBottle", "minecraft:xp_bottle"),
            Map.entry("ThrownPotion", "minecraft:potion"),
            Map.entry("Villager", "minecraft:villager"),
            Map.entry("VillagerGolem", "minecraft:villager_golem"),
            Map.entry("Witch", "minecraft:witch"),
            Map.entry("WitherBoss", "minecraft:wither"),
            Map.entry("WitherSkeleton", "minecraft:wither_skeleton"),
            Map.entry("WitherSkull", "minecraft:wither_skull"),
            Map.entry("Wolf", "minecraft:wolf"),
            Map.entry("XPOrb", "minecraft:xp_orb"),
            Map.entry("Zombie", "minecraft:zombie"),
            Map.entry("ZombieHorse", "minecraft:zombie_horse"),
            Map.entry("ZombieVillager", "minecraft:zombie_villager")
        );
    }
}
