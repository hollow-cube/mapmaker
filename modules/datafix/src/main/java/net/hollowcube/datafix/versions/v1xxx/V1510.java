package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.fixes.SimpleEntityRenameFix;

import java.util.HashMap;
import java.util.Map;

public class V1510 extends DataVersion {
    public static final Map<String, String> RENAMED_ENTITIES;
    public static final Map<String, String> RENAMED_BLOCKS;
    public static final Map<String, String> RENAMED_ITEMS;
    private static final String MINECRAFT_BRED = "minecraft:bred_";

    public V1510() {
        super(1510);

        renameReference(DataTypes.ENTITY, "minecraft:commandblock_minecart", "minecraft:command_block_minecart");
        renameReference(DataTypes.ENTITY, "minecraft:ender_crystal", "minecraft:end_crystal");
        renameReference(DataTypes.ENTITY, "minecraft:snowman", "minecraft:snow_golem");
        renameReference(DataTypes.ENTITY, "minecraft:evocation_illager", "minecraft:evoker");
        renameReference(DataTypes.ENTITY, "minecraft:evocation_fangs", "minecraft:evoker_fangs");
        renameReference(DataTypes.ENTITY, "minecraft:illusion_illager", "minecraft:illusioner");
        renameReference(DataTypes.ENTITY, "minecraft:vindication_illager", "minecraft:vindicator");
        renameReference(DataTypes.ENTITY, "minecraft:villager_golem", "minecraft:iron_golem");
        renameReference(DataTypes.ENTITY, "minecraft:xp_orb", "minecraft:experience_orb");
        renameReference(DataTypes.ENTITY, "minecraft:xp_bottle", "minecraft:experience_bottle");
        renameReference(DataTypes.ENTITY, "minecraft:eye_of_ender_signal", "minecraft:eye_of_ender");
        renameReference(DataTypes.ENTITY, "minecraft:fireworks_rocket", "minecraft:firework_rocket");

        var blockFix = new BlockRenameFix(RENAMED_BLOCKS);
        addFix(DataTypes.BLOCK_NAME, blockFix);
        addFix(DataTypes.BLOCK_STATE, blockFix);
        addFix(DataTypes.FLAT_BLOCK_STATE, blockFix);
        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMED_ITEMS));
        addFix(DataTypes.ENTITY_NAME, new SimpleEntityRenameFix(RENAMED_ENTITIES));
    }

    static {
        RENAMED_ENTITIES = Map.ofEntries(
                Map.entry("minecraft:commandblock_minecart", "minecraft:command_block_minecart"),
                Map.entry("minecraft:ender_crystal", "minecraft:end_crystal"),
                Map.entry("minecraft:snowman", "minecraft:snow_golem"),
                Map.entry("minecraft:evocation_illager", "minecraft:evoker"),
                Map.entry("minecraft:evocation_fangs", "minecraft:evoker_fangs"),
                Map.entry("minecraft:illusion_illager", "minecraft:illusioner"),
                Map.entry("minecraft:vindication_illager", "minecraft:vindicator"),
                Map.entry("minecraft:villager_golem", "minecraft:iron_golem"),
                Map.entry("minecraft:xp_orb", "minecraft:experience_orb"),
                Map.entry("minecraft:xp_bottle", "minecraft:experience_bottle"),
                Map.entry("minecraft:eye_of_ender_signal", "minecraft:eye_of_ender"),
                Map.entry("minecraft:fireworks_rocket", "minecraft:firework_rocket")
        );
        RENAMED_BLOCKS = Map.ofEntries(
                Map.entry("minecraft:portal", "minecraft:nether_portal"),
                Map.entry("minecraft:oak_bark", "minecraft:oak_wood"),
                Map.entry("minecraft:spruce_bark", "minecraft:spruce_wood"),
                Map.entry("minecraft:birch_bark", "minecraft:birch_wood"),
                Map.entry("minecraft:jungle_bark", "minecraft:jungle_wood"),
                Map.entry("minecraft:acacia_bark", "minecraft:acacia_wood"),
                Map.entry("minecraft:dark_oak_bark", "minecraft:dark_oak_wood"),
                Map.entry("minecraft:stripped_oak_bark", "minecraft:stripped_oak_wood"),
                Map.entry("minecraft:stripped_spruce_bark", "minecraft:stripped_spruce_wood"),
                Map.entry("minecraft:stripped_birch_bark", "minecraft:stripped_birch_wood"),
                Map.entry("minecraft:stripped_jungle_bark", "minecraft:stripped_jungle_wood"),
                Map.entry("minecraft:stripped_acacia_bark", "minecraft:stripped_acacia_wood"),
                Map.entry("minecraft:stripped_dark_oak_bark", "minecraft:stripped_dark_oak_wood"),
                Map.entry("minecraft:mob_spawner", "minecraft:spawner")
        );
        var renamedItems = new HashMap<>(RENAMED_BLOCKS);
        renamedItems.put("minecraft:clownfish", "minecraft:tropical_fish");
        renamedItems.put("minecraft:chorus_fruit_popped", "minecraft:popped_chorus_fruit");
        renamedItems.put("minecraft:evocation_illager_spawn_egg", "minecraft:evoker_spawn_egg");
        renamedItems.put("minecraft:vindication_illager_spawn_egg", "minecraft:vindicator_spawn_egg");
        RENAMED_ITEMS = Map.copyOf(renamedItems);
    }
}
