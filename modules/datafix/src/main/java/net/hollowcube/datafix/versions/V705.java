package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V705 extends DataVersion {
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
    }

    private void addProjectileEntity(@NotNull String id) {
        addReference(DataType.ENTITY, id, field -> field
                .single("inTile", DataType.BLOCK_NAME));
    }
}
