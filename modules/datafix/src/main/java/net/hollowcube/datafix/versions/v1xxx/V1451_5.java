package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class V1451_5 extends DataVersion {
    private static final Set<String> ITEMS;
    private static final Map<String, String> ENTITY_TO_SPAWN_EGG;

    public V1451_5() {
        super(1451, 5);

        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:flower_pot");
        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:noteblock");

        ITEMS.forEach(id -> addFix(DataTypes.ITEM_STACK, id, V1451_5::fixRemoveBlockEntityTag));
        addFix(DataTypes.ITEM_STACK, "minecraft:spawn_egg", V1451_5::fixSpawnEggItemId);
        // TODO: This fix is disabled in paper, not sure if we want it or not.
        addFix(DataTypes.ENTITY, "minecraft:wolf", V1451_5::fixWolfEntityColor);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:banner", V1451_5::fixBannerBlockEntityColor);
    }

    private static @Nullable Value fixRemoveBlockEntityTag(Value itemStack) {
        itemStack.get("tag").remove("BlockEntityTag");
        return null;
    }

    private static @Nullable Value fixSpawnEggItemId(Value itemStack) {
        var entityId = itemStack.get("tag", Value::emptyMap)
                .get("EntityTag", Value::emptyMap)
                .getValue("id");
        if (entityId instanceof String s) {
            var spawnEggId = ENTITY_TO_SPAWN_EGG.get(s);
            if (spawnEggId != null) itemStack.put("id", spawnEggId);
        }

        return null;
    }

    private static @Nullable Value fixWolfEntityColor(Value entity) {
        var collarColor = entity.get("CollarColor").as(Number.class, 0).intValue();
        entity.put("CollarColor", 15 - collarColor);
        return null;
    }

    private static @Nullable Value fixBannerBlockEntityColor(Value blockEntity) {
        int baseColor = blockEntity.get("Base").as(Number.class, 0).intValue();
        blockEntity.put("Base", 15 - baseColor);

        for (var pattern : blockEntity.get("Patterns")) {
            int color = pattern.get("Color").as(Number.class, 0).intValue();
            pattern.put("Color", 15 - color);
        }

        return null;
    }

    static {
        ITEMS = Set.of(
            "minecraft:noteblock",
            "minecraft:flower_pot",
            "minecraft:dandelion",
            "minecraft:poppy",
            "minecraft:blue_orchid",
            "minecraft:allium",
            "minecraft:azure_bluet",
            "minecraft:red_tulip",
            "minecraft:orange_tulip",
            "minecraft:white_tulip",
            "minecraft:pink_tulip",
            "minecraft:oxeye_daisy",
            "minecraft:cactus",
            "minecraft:brown_mushroom",
            "minecraft:red_mushroom",
            "minecraft:oak_sapling",
            "minecraft:spruce_sapling",
            "minecraft:birch_sapling",
            "minecraft:jungle_sapling",
            "minecraft:acacia_sapling",
            "minecraft:dark_oak_sapling",
            "minecraft:dead_bush",
            "minecraft:fern"
        );
        ENTITY_TO_SPAWN_EGG = Map.ofEntries(
            Map.entry("minecraft:bat", "minecraft:bat_spawn_egg"),
            Map.entry("minecraft:blaze", "minecraft:blaze_spawn_egg"),
            Map.entry("minecraft:cave_spider", "minecraft:cave_spider_spawn_egg"),
            Map.entry("minecraft:chicken", "minecraft:chicken_spawn_egg"),
            Map.entry("minecraft:cow", "minecraft:cow_spawn_egg"),
            Map.entry("minecraft:creeper", "minecraft:creeper_spawn_egg"),
            Map.entry("minecraft:donkey", "minecraft:donkey_spawn_egg"),
            Map.entry("minecraft:elder_guardian", "minecraft:elder_guardian_spawn_egg"),
            Map.entry("minecraft:ender_dragon", "minecraft:ender_dragon_spawn_egg"),
            Map.entry("minecraft:enderman", "minecraft:enderman_spawn_egg"),
            Map.entry("minecraft:endermite", "minecraft:endermite_spawn_egg"),
            Map.entry("minecraft:evocation_illager", "minecraft:evocation_illager_spawn_egg"),
            Map.entry("minecraft:ghast", "minecraft:ghast_spawn_egg"),
            Map.entry("minecraft:guardian", "minecraft:guardian_spawn_egg"),
            Map.entry("minecraft:horse", "minecraft:horse_spawn_egg"),
            Map.entry("minecraft:husk", "minecraft:husk_spawn_egg"),
            Map.entry("minecraft:iron_golem", "minecraft:iron_golem_spawn_egg"),
            Map.entry("minecraft:llama", "minecraft:llama_spawn_egg"),
            Map.entry("minecraft:magma_cube", "minecraft:magma_cube_spawn_egg"),
            Map.entry("minecraft:mooshroom", "minecraft:mooshroom_spawn_egg"),
            Map.entry("minecraft:mule", "minecraft:mule_spawn_egg"),
            Map.entry("minecraft:ocelot", "minecraft:ocelot_spawn_egg"),
            Map.entry("minecraft:pufferfish", "minecraft:pufferfish_spawn_egg"),
            Map.entry("minecraft:parrot", "minecraft:parrot_spawn_egg"),
            Map.entry("minecraft:pig", "minecraft:pig_spawn_egg"),
            Map.entry("minecraft:polar_bear", "minecraft:polar_bear_spawn_egg"),
            Map.entry("minecraft:rabbit", "minecraft:rabbit_spawn_egg"),
            Map.entry("minecraft:sheep", "minecraft:sheep_spawn_egg"),
            Map.entry("minecraft:shulker", "minecraft:shulker_spawn_egg"),
            Map.entry("minecraft:silverfish", "minecraft:silverfish_spawn_egg"),
            Map.entry("minecraft:skeleton", "minecraft:skeleton_spawn_egg"),
            Map.entry("minecraft:skeleton_horse", "minecraft:skeleton_horse_spawn_egg"),
            Map.entry("minecraft:slime", "minecraft:slime_spawn_egg"),
            Map.entry("minecraft:snow_golem", "minecraft:snow_golem_spawn_egg"),
            Map.entry("minecraft:spider", "minecraft:spider_spawn_egg"),
            Map.entry("minecraft:squid", "minecraft:squid_spawn_egg"),
            Map.entry("minecraft:stray", "minecraft:stray_spawn_egg"),
            Map.entry("minecraft:turtle", "minecraft:turtle_spawn_egg"),
            Map.entry("minecraft:vex", "minecraft:vex_spawn_egg"),
            Map.entry("minecraft:villager", "minecraft:villager_spawn_egg"),
            Map.entry("minecraft:vindication_illager", "minecraft:vindication_illager_spawn_egg"),
            Map.entry("minecraft:witch", "minecraft:witch_spawn_egg"),
            Map.entry("minecraft:wither", "minecraft:wither_spawn_egg"),
            Map.entry("minecraft:wither_skeleton", "minecraft:wither_skeleton_spawn_egg"),
            Map.entry("minecraft:wolf", "minecraft:wolf_spawn_egg"),
            Map.entry("minecraft:zombie", "minecraft:zombie_spawn_egg"),
            Map.entry("minecraft:zombie_horse", "minecraft:zombie_horse_spawn_egg"),
            Map.entry("minecraft:zombie_pigman", "minecraft:zombie_pigman_spawn_egg"),
            Map.entry("minecraft:zombie_villager", "minecraft:zombie_villager_spawn_egg")
        );
    }
}
