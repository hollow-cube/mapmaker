package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class V705 extends DataVersion {
    private static final Map<String, String> ENTITY_ID_MAP;

    public V705() {
        super(705);

        addReference(DataType.ENTITY, "minecraft:area_effect_cloud");
        addReference(DataType.ENTITY, "minecraft:armor_stand");
        addReference(DataType.ENTITY, "minecraft:arrow", field -> field
                .single("inTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:bat");
        addReference(DataType.ENTITY, "minecraft:blaze");
        addReference(DataType.ENTITY, "minecraft:boat");
        addReference(DataType.ENTITY, "minecraft:cave_spider");
        addReference(DataType.ENTITY, "minecraft:chest_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME)
                .list("Items", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:chicken");
        addReference(DataType.ENTITY, "minecraft:commandblock_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME)
                .single("LastOutput", DataType.TEXT_COMPONENT));
        addReference(DataType.ENTITY, "minecraft:cow");
        addReference(DataType.ENTITY, "minecraft:creeper");
        addReference(DataType.ENTITY, "minecraft:donkey", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:dragon_fireball");
        addProjectileEntity("minecraft:egg");
        addReference(DataType.ENTITY, "minecraft:elder_guardian");
        addReference(DataType.ENTITY, "minecraft:ender_crystal");
        addReference(DataType.ENTITY, "minecraft:ender_dragon");
        addReference(DataType.ENTITY, "minecraft:enderman", field -> field
                .single("carried", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:endermite");
        addProjectileEntity("minecraft:ender_pearl");
        addReference(DataType.ENTITY, "minecraft:eye_of_ender_signal");
        addReference(DataType.ENTITY, "minecraft:falling_block", field -> field
                .single("Block", DataType.BLOCK_NAME)
                .single("TileEntityData", DataType.BLOCK_ENTITY));
        addProjectileEntity("minecraft:fireball");
        addReference(DataType.ENTITY, "minecraft:fireworks_rocket", field -> field
                .single("FireworksItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:furnace_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:ghast");
        addReference(DataType.ENTITY, "minecraft:giant");
        addReference(DataType.ENTITY, "minecraft:guardian");
        addReference(DataType.ENTITY, "minecraft:hopper_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME)
                .list("Items", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:horse", field -> field
                .single("ArmorItem", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:husk");
        addReference(DataType.ENTITY, "minecraft:item", field -> field
                .single("Item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:item_frame", field -> field
                .single("Item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:leash_knot");
        addReference(DataType.ENTITY, "minecraft:magma_cube");
        addReference(DataType.ENTITY, "minecraft:minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:mooshroom");
        addReference(DataType.ENTITY, "minecraft:mule", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:ocelot");
        addReference(DataType.ENTITY, "minecraft:painting");
        addReference(DataType.ENTITY, "minecraft:parrot");
        addReference(DataType.ENTITY, "minecraft:pig");
        addReference(DataType.ENTITY, "minecraft:polar_bear");
        addReference(DataType.ENTITY, "minecraft:potion", field -> field
                .single("Potion", DataType.ITEM_STACK)
                .single("inTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:rabbit");
        addReference(DataType.ENTITY, "minecraft:sheep");
        addReference(DataType.ENTITY, "minecraft:shulker");
        addReference(DataType.ENTITY, "minecraft:shulker_bullet");
        addReference(DataType.ENTITY, "minecraft:silverfish");
        addReference(DataType.ENTITY, "minecraft:skeleton");
        addReference(DataType.ENTITY, "minecraft:skeleton_horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:slime");
        addProjectileEntity("minecraft:small_fireball");
        addProjectileEntity("minecraft:snowball");
        addReference(DataType.ENTITY, "minecraft:snowman");
        addReference(DataType.ENTITY, "minecraft:spawner_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:spider");
        addReference(DataType.ENTITY, "minecraft:squid");
        addReference(DataType.ENTITY, "minecraft:stray");
        addReference(DataType.ENTITY, "minecraft:tnt");
        addReference(DataType.ENTITY, "minecraft:tnt_minecart", field -> field
                .single("DisplayTile", DataType.BLOCK_NAME));
        addReference(DataType.ENTITY, "minecraft:villager", field -> field
                .list("Inventory", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:villager_golem");
        addReference(DataType.ENTITY, "minecraft:witch");
        addReference(DataType.ENTITY, "minecraft:wither");
        addReference(DataType.ENTITY, "minecraft:wither_skeleton");
        addProjectileEntity("minecraft:wither_skull");
        addReference(DataType.ENTITY, "minecraft:wolf");
        addProjectileEntity("minecraft:xp_bottle");
        addReference(DataType.ENTITY, "minecraft:xp_orb");
        addReference(DataType.ENTITY, "minecraft:zombie");
        addReference(DataType.ENTITY, "minecraft:zombie_horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:zombie_pigman");
        addReference(DataType.ENTITY, "minecraft:zombie_villager");
        addReference(DataType.ENTITY, "minecraft:evocation_fangs");
        addReference(DataType.ENTITY, "minecraft:evocation_illager");
        addReference(DataType.ENTITY, "minecraft:illusion_illager");
        addReference(DataType.ENTITY, "minecraft:llama", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK)
                .single("DecorItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:llama_spit");
        addReference(DataType.ENTITY, "minecraft:vex");
        addReference(DataType.ENTITY, "minecraft:vindication_illager");

        addFix(DataType.ENTITY, new EntityRenameFix(V705::fixEntityId));
    }

    private void addProjectileEntity(@NotNull String id) {
        addReference(DataType.ENTITY, id, field -> field
                .single("inTile", DataType.BLOCK_NAME));
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
