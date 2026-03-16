package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.datafix.versions.v0xxx.V99;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class V1460 extends DataVersion {
    private static final Map<String, String> PAINTING_MOTIVE_MAP = Map.of(
        "donkeykong", "donkey_kong",
        "burningskull", "burning_skull",
        "skullandroses", "skull_and_roses"
    );

    public V1460() {
        super(1460);

        registerEntities();
        registerBlockEntities();

        addFix(DataTypes.ENTITY, "minecraft:painting", V1460::fixEntityPaintingMotive);
    }

    private void registerEntities() {
        // TODO: these seem like all reregistrations, probably can exclude.
        addReference(DataTypes.ENTITY, "minecraft:area_effect_cloud");
        addReference(DataTypes.ENTITY, "minecraft:armor_stand");
        addReference(DataTypes.ENTITY, "minecraft:arrow", field -> field
            .single("inBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:bat");
        addReference(DataTypes.ENTITY, "minecraft:blaze");
        addReference(DataTypes.ENTITY, "minecraft:boat");
        addReference(DataTypes.ENTITY, "minecraft:cave_spider");
        addReference(DataTypes.ENTITY, "minecraft:chest_minecart", field -> field
            .single("inBlockState", DataTypes.BLOCK_STATE)
            .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:chicken");
        addReference(DataTypes.ENTITY, "minecraft:commandblock_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE)
            .single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.ENTITY, "minecraft:cow");
        addReference(DataTypes.ENTITY, "minecraft:creeper");
        addReference(DataTypes.ENTITY, "minecraft:donkey", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:dragon_fireball");
        addReference(DataTypes.ENTITY, "minecraft:egg");
        addReference(DataTypes.ENTITY, "minecraft:elder_guardian");
        addReference(DataTypes.ENTITY, "minecraft:ender_crystal");
        addReference(DataTypes.ENTITY, "minecraft:ender_dragon");
        addReference(DataTypes.ENTITY, "minecraft:enderman", field -> field
            .single("carriedBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:endermite");
        addReference(DataTypes.ENTITY, "minecraft:ender_pearl");
        addReference(DataTypes.ENTITY, "minecraft:evocation_fangs");
        addReference(DataTypes.ENTITY, "minecraft:evocation_illager");
        addReference(DataTypes.ENTITY, "minecraft:eye_of_ender_signal");
        addReference(DataTypes.ENTITY, "minecraft:falling_block", field -> field
            .single("BlockState", DataTypes.BLOCK_STATE)
            .single("TileEntityData", DataTypes.BLOCK_ENTITY));
        addReference(DataTypes.ENTITY, "minecraft:fireball");
        addReference(DataTypes.ENTITY, "minecraft:fireworks_rocket", field -> field
            .single("FireworksItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:furnace_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:ghast");
        addReference(DataTypes.ENTITY, "minecraft:giant");
        addReference(DataTypes.ENTITY, "minecraft:guardian");
        addReference(DataTypes.ENTITY, "minecraft:hopper_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE)
            .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:horse", field -> field
            .single("ArmorItem", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:husk");
        addReference(DataTypes.ENTITY, "minecraft:illusion_illager");
        addReference(DataTypes.ENTITY, "minecraft:item", field -> field
            .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:item_frame", field -> field
            .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:leash_knot");
        addReference(DataTypes.ENTITY, "minecraft:llama", field -> field
            .list("Items", DataTypes.ITEM_STACK)
            .single("SaddleItem", DataTypes.ITEM_STACK)
            .single("DecorItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:llama_spit");
        addReference(DataTypes.ENTITY, "minecraft:magma_cube");
        addReference(DataTypes.ENTITY, "minecraft:minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
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
            .single("Potion", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:rabbit");
        addReference(DataTypes.ENTITY, "minecraft:sheep");
        addReference(DataTypes.ENTITY, "minecraft:shulker");
        addReference(DataTypes.ENTITY, "minecraft:shulker_bullet");
        addReference(DataTypes.ENTITY, "minecraft:silverfish");
        addReference(DataTypes.ENTITY, "minecraft:skeleton");
        addReference(DataTypes.ENTITY, "minecraft:skeleton_horse", field -> field
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:slime");
        addReference(DataTypes.ENTITY, "minecraft:small_fireball");
        addReference(DataTypes.ENTITY, "minecraft:snowball");
        addReference(DataTypes.ENTITY, "minecraft:snowman");
        addReference(DataTypes.ENTITY, "minecraft:spawner_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:spectral_arrow");
        addReference(DataTypes.ENTITY, "minecraft:spider");
        addReference(DataTypes.ENTITY, "minecraft:squid");
        addReference(DataTypes.ENTITY, "minecraft:stray");
        addReference(DataTypes.ENTITY, "minecraft:tnt");
        addReference(DataTypes.ENTITY, "minecraft:tnt_minecart", field -> field
            .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:vex");
        addReference(DataTypes.ENTITY, "minecraft:villager", field -> field
            .list("Inventory", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:villager_golem");
        addReference(DataTypes.ENTITY, "minecraft:vindication_illager");
        addReference(DataTypes.ENTITY, "minecraft:witch");
        addReference(DataTypes.ENTITY, "minecraft:wither");
        addReference(DataTypes.ENTITY, "minecraft:wither_skeleton");
        addReference(DataTypes.ENTITY, "minecraft:wither_skull");
        addReference(DataTypes.ENTITY, "minecraft:wolf");
        addReference(DataTypes.ENTITY, "minecraft:xp_bottle");
        addReference(DataTypes.ENTITY, "minecraft:xp_orb");
        addReference(DataTypes.ENTITY, "minecraft:zombie");
        addReference(DataTypes.ENTITY, "minecraft:zombie_horse", field -> field
            .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:zombie_pigman");
        addReference(DataTypes.ENTITY, "minecraft:zombie_villager");
    }

    private void registerBlockEntities() {
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:furnace", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:chest", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:trapped_chest", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:ender_chest");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:jukebox", field -> field
            .single("RecordItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:dispenser", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:dropper", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:sign", V99::signBlock);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:mob_spawner");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:piston");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:brewing_stand", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:enchanting_table", V1458::nameable);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:end_portal");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:beacon", V1458::nameable);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:skull", field -> field
            .single("custom_name", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:daylight_detector");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:hopper", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:comparator");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:banner", V1458::nameable);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:structure_block");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:end_gateway");
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:command_block", field -> field.single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:shulker_box", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:bed");
    }

    private static @Nullable Value fixEntityPaintingMotive(Value value) {
        if (!(value.getValue("Motive") instanceof String motive))
            return null;

        value.put("Motive", namespaced(PAINTING_MOTIVE_MAP.getOrDefault(motive, motive)));
        return null;
    }
}
