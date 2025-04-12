package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

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

        addFix(DataType.ENTITY, "minecraft:painting", V1460::fixEntityPaintingMotive);
    }

    private void registerEntities() {
        addReference(DataType.ENTITY, "minecraft:area_effect_cloud");
        addReference(DataType.ENTITY, "minecraft:armor_stand");
        addReference(DataType.ENTITY, "minecraft:arrow", field -> field
                .single("inBlockState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:bat");
        addReference(DataType.ENTITY, "minecraft:blaze");
        addReference(DataType.ENTITY, "minecraft:boat");
        addReference(DataType.ENTITY, "minecraft:cave_spider");
        addReference(DataType.ENTITY, "minecraft:chest_minecart", field -> field
                .single("inBlockState", DataType.BLOCK_STATE)
                .list("Items", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:chicken");
        addReference(DataType.ENTITY, "minecraft:commandblock_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE)
                .single("LastOutput", DataType.TEXT_COMPONENT));
        addReference(DataType.ENTITY, "minecraft:cow");
        addReference(DataType.ENTITY, "minecraft:creeper");
        addReference(DataType.ENTITY, "minecraft:donkey", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:dragon_fireball");
        addReference(DataType.ENTITY, "minecraft:egg");
        addReference(DataType.ENTITY, "minecraft:elder_guardian");
        addReference(DataType.ENTITY, "minecraft:ender_crystal");
        addReference(DataType.ENTITY, "minecraft:ender_dragon");
        addReference(DataType.ENTITY, "minecraft:enderman", field -> field
                .single("carriedBlockState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:endermite");
        addReference(DataType.ENTITY, "minecraft:ender_pearl");
        addReference(DataType.ENTITY, "minecraft:evocation_fangs");
        addReference(DataType.ENTITY, "minecraft:evocation_illager");
        addReference(DataType.ENTITY, "minecraft:eye_of_ender_signal");
        addReference(DataType.ENTITY, "minecraft:falling_block", field -> field
                .single("BlockState", DataType.BLOCK_STATE)
                .single("TileEntityData", DataType.BLOCK_ENTITY));
        addReference(DataType.ENTITY, "minecraft:fireball");
        addReference(DataType.ENTITY, "minecraft:fireworks_rocket", field -> field
                .single("FireworksItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:furnace_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:ghast");
        addReference(DataType.ENTITY, "minecraft:giant");
        addReference(DataType.ENTITY, "minecraft:guardian");
        addReference(DataType.ENTITY, "minecraft:hopper_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE)
                .list("Items", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:horse", field -> field
                .single("ArmorItem", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:husk");
        addReference(DataType.ENTITY, "minecraft:illusion_illager");
        addReference(DataType.ENTITY, "minecraft:item", field -> field
                .single("Item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:item_frame", field -> field
                .single("Item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:leash_knot");
        addReference(DataType.ENTITY, "minecraft:llama", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK)
                .single("DecorItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:llama_spit");
        addReference(DataType.ENTITY, "minecraft:magma_cube");
        addReference(DataType.ENTITY, "minecraft:minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
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
                .single("Potion", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:rabbit");
        addReference(DataType.ENTITY, "minecraft:sheep");
        addReference(DataType.ENTITY, "minecraft:shulker");
        addReference(DataType.ENTITY, "minecraft:shulker_bullet");
        addReference(DataType.ENTITY, "minecraft:silverfish");
        addReference(DataType.ENTITY, "minecraft:skeleton");
        addReference(DataType.ENTITY, "minecraft:skeleton_horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:slime");
        addReference(DataType.ENTITY, "minecraft:small_fireball");
        addReference(DataType.ENTITY, "minecraft:snowball");
        addReference(DataType.ENTITY, "minecraft:snowman");
        addReference(DataType.ENTITY, "minecraft:spawner_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:spectral_arrow");
        addReference(DataType.ENTITY, "minecraft:spider");
        addReference(DataType.ENTITY, "minecraft:squid");
        addReference(DataType.ENTITY, "minecraft:stray");
        addReference(DataType.ENTITY, "minecraft:tnt");
        addReference(DataType.ENTITY, "minecraft:tnt_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:vex");
        addReference(DataType.ENTITY, "minecraft:villager", field -> field
                .list("Inventory", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:villager_golem");
        addReference(DataType.ENTITY, "minecraft:vindication_illager");
        addReference(DataType.ENTITY, "minecraft:witch");
        addReference(DataType.ENTITY, "minecraft:wither");
        addReference(DataType.ENTITY, "minecraft:wither_skeleton");
        addReference(DataType.ENTITY, "minecraft:wither_skull");
        addReference(DataType.ENTITY, "minecraft:wolf");
        addReference(DataType.ENTITY, "minecraft:xp_bottle");
        addReference(DataType.ENTITY, "minecraft:xp_orb");
        addReference(DataType.ENTITY, "minecraft:zombie");
        addReference(DataType.ENTITY, "minecraft:zombie_horse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:zombie_pigman");
        addReference(DataType.ENTITY, "minecraft:zombie_villager");
    }

    private void registerBlockEntities() {
        addReference(DataType.BLOCK_ENTITY, "minecraft:furnace", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:chest", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:trapped_chest", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:ender_chest");
        addReference(DataType.BLOCK_ENTITY, "minecraft:jukebox", field -> field
                .single("RecordItem", DataType.ITEM_STACK));
        addReference(DataType.BLOCK_ENTITY, "minecraft:dispenser", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:dropper", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:sign", V99::signBlock);
        addReference(DataType.BLOCK_ENTITY, "minecraft:mob_spawner");
        addReference(DataType.BLOCK_ENTITY, "minecraft:piston");
        addReference(DataType.BLOCK_ENTITY, "minecraft:brewing_stand", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:enchanting_table", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:end_portal");
        addReference(DataType.BLOCK_ENTITY, "minecraft:beacon", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:skull", field -> field
                .single("custom_name", DataType.TEXT_COMPONENT));
        addReference(DataType.BLOCK_ENTITY, "minecraft:daylight_detector");
        addReference(DataType.BLOCK_ENTITY, "minecraft:hopper", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:comparator");
        addReference(DataType.BLOCK_ENTITY, "minecraft:banner", V1458::nameable);
        addReference(DataType.BLOCK_ENTITY, "minecraft:structure_block");
        addReference(DataType.BLOCK_ENTITY, "minecraft:end_gateway");
        addReference(DataType.BLOCK_ENTITY, "minecraft:command_block", field -> field.single("LastOutput", DataType.TEXT_COMPONENT));
        addReference(DataType.BLOCK_ENTITY, "minecraft:shulker_box", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:bed");
    }

    private static Value fixEntityPaintingMotive(Value value) {
        if (!(value.getValue("Motive") instanceof String motive))
            return null;

        value.put("Motive", namespaced(PAINTING_MOTIVE_MAP.getOrDefault(motive, motive)));
        return null;
    }
}
