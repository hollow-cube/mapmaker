package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class V1451_4 extends DataVersion {
    private static final Map<String, String> ITEM_WITH_DAMAGE_RENAMES;
    private static final Set<String> ITEM_IDS;
    private static final Set<String> ITEMS_WITH_DURABILITY;

    public V1451_4() {
        super(1451, 4);

        addFix(DataTypes.BLOCK_NAME, V1451_4::fixBlockNameFlattening);
        addFix(DataTypes.ITEM_STACK, V1451_4::fixItemStackNameFlattening);
    }

    private static @Nullable Value fixBlockNameFlattening(Value blockName) {
        var raw = blockName.value();
        if (raw instanceof String id) {
            return V1450.upgradeBlockName(namespaced(id));
        } else if (raw instanceof Number idNumber) {
            return V1450.upgradeBlockId(idNumber.intValue());
        } else return null;
    }

    private static @Nullable Value fixItemStackNameFlattening(Value itemStack) {
        var id = itemStack.get("id").as(String.class, null);
        if (id == null) return null;

        int damage = itemStack.remove("Damage").as(Number.class, 0).intValue();
        String newId = updateItemId(id, damage);
        if (newId != null) itemStack.put("id", newId);

        if (ITEMS_WITH_DURABILITY.contains(id)) {
            itemStack.get("tag", Value::emptyMap).put("Damage", damage);
        }

        return null;
    }

    static @Nullable String updateItemId(@Nullable String id, int damage) {
        if (id == null || !ITEM_IDS.contains(id)) return null;
        var damageId = ITEM_WITH_DAMAGE_RENAMES.get(id + "." + damage);
        if (damageId != null) return damageId;
        return ITEM_WITH_DAMAGE_RENAMES.get(id + ".0");
    }

    static {
        ITEM_WITH_DAMAGE_RENAMES = new HashMap<>();
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.0", "minecraft:stone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.1", "minecraft:granite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.2", "minecraft:polished_granite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.3", "minecraft:diorite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.4", "minecraft:polished_diorite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.5", "minecraft:andesite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone.6", "minecraft:polished_andesite");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dirt.0", "minecraft:dirt");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dirt.1", "minecraft:coarse_dirt");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dirt.2", "minecraft:podzol");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves.0", "minecraft:oak_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves.1", "minecraft:spruce_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves.2", "minecraft:birch_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves.3", "minecraft:jungle_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves2.0", "minecraft:acacia_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:leaves2.1", "minecraft:dark_oak_leaves");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log.0", "minecraft:oak_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log.1", "minecraft:spruce_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log.2", "minecraft:birch_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log.3", "minecraft:jungle_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log2.0", "minecraft:acacia_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:log2.1", "minecraft:dark_oak_log");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.0", "minecraft:oak_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.1", "minecraft:spruce_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.2", "minecraft:birch_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.3", "minecraft:jungle_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.4", "minecraft:acacia_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sapling.5", "minecraft:dark_oak_sapling");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.0", "minecraft:oak_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.1", "minecraft:spruce_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.2", "minecraft:birch_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.3", "minecraft:jungle_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.4", "minecraft:acacia_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:planks.5", "minecraft:dark_oak_planks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sand.0", "minecraft:sand");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sand.1", "minecraft:red_sand");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:quartz_block.0", "minecraft:quartz_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:quartz_block.1", "minecraft:chiseled_quartz_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:quartz_block.2", "minecraft:quartz_pillar");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:anvil.0", "minecraft:anvil");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:anvil.1", "minecraft:chipped_anvil");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:anvil.2", "minecraft:damaged_anvil");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.0", "minecraft:white_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.1", "minecraft:orange_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.2", "minecraft:magenta_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.3", "minecraft:light_blue_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.4", "minecraft:yellow_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.5", "minecraft:lime_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.6", "minecraft:pink_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.7", "minecraft:gray_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.8", "minecraft:light_gray_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.9", "minecraft:cyan_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.10", "minecraft:purple_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.11", "minecraft:blue_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.12", "minecraft:brown_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.13", "minecraft:green_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.14", "minecraft:red_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wool.15", "minecraft:black_wool");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.0", "minecraft:white_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.1", "minecraft:orange_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.2", "minecraft:magenta_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.3", "minecraft:light_blue_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.4", "minecraft:yellow_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.5", "minecraft:lime_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.6", "minecraft:pink_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.7", "minecraft:gray_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.8", "minecraft:light_gray_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.9", "minecraft:cyan_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.10", "minecraft:purple_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.11", "minecraft:blue_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.12", "minecraft:brown_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.13", "minecraft:green_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.14", "minecraft:red_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:carpet.15", "minecraft:black_carpet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:hardened_clay.0", "minecraft:terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.0", "minecraft:white_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.1", "minecraft:orange_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.2", "minecraft:magenta_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.3", "minecraft:light_blue_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.4", "minecraft:yellow_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.5", "minecraft:lime_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.6", "minecraft:pink_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.7", "minecraft:gray_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.8", "minecraft:light_gray_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.9", "minecraft:cyan_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.10", "minecraft:purple_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.11", "minecraft:blue_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.12", "minecraft:brown_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.13", "minecraft:green_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.14", "minecraft:red_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_hardened_clay.15", "minecraft:black_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:silver_glazed_terracotta.0", "minecraft:light_gray_glazed_terracotta");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.0", "minecraft:white_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.1", "minecraft:orange_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.2", "minecraft:magenta_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.3", "minecraft:light_blue_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.4", "minecraft:yellow_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.5", "minecraft:lime_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.6", "minecraft:pink_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.7", "minecraft:gray_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.8", "minecraft:light_gray_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.9", "minecraft:cyan_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.10", "minecraft:purple_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.11", "minecraft:blue_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.12", "minecraft:brown_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.13", "minecraft:green_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.14", "minecraft:red_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass.15", "minecraft:black_stained_glass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.0", "minecraft:white_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.1", "minecraft:orange_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.2", "minecraft:magenta_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.3", "minecraft:light_blue_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.4", "minecraft:yellow_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.5", "minecraft:lime_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.6", "minecraft:pink_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.7", "minecraft:gray_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.8", "minecraft:light_gray_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.9", "minecraft:cyan_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.10", "minecraft:purple_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.11", "minecraft:blue_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.12", "minecraft:brown_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.13", "minecraft:green_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.14", "minecraft:red_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stained_glass_pane.15", "minecraft:black_stained_glass_pane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:prismarine.0", "minecraft:prismarine");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:prismarine.1", "minecraft:prismarine_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:prismarine.2", "minecraft:dark_prismarine");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.0", "minecraft:white_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.1", "minecraft:orange_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.2", "minecraft:magenta_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.3", "minecraft:light_blue_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.4", "minecraft:yellow_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.5", "minecraft:lime_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.6", "minecraft:pink_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.7", "minecraft:gray_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.8", "minecraft:light_gray_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.9", "minecraft:cyan_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.10", "minecraft:purple_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.11", "minecraft:blue_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.12", "minecraft:brown_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.13", "minecraft:green_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.14", "minecraft:red_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete.15", "minecraft:black_concrete");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.0", "minecraft:white_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.1", "minecraft:orange_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.2", "minecraft:magenta_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.3", "minecraft:light_blue_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.4", "minecraft:yellow_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.5", "minecraft:lime_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.6", "minecraft:pink_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.7", "minecraft:gray_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.8", "minecraft:light_gray_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.9", "minecraft:cyan_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.10", "minecraft:purple_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.11", "minecraft:blue_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.12", "minecraft:brown_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.13", "minecraft:green_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.14", "minecraft:red_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:concrete_powder.15", "minecraft:black_concrete_powder");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:cobblestone_wall.0", "minecraft:cobblestone_wall");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:cobblestone_wall.1", "minecraft:mossy_cobblestone_wall");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sandstone.0", "minecraft:sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sandstone.1", "minecraft:chiseled_sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sandstone.2", "minecraft:cut_sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_sandstone.0", "minecraft:red_sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_sandstone.1", "minecraft:chiseled_red_sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_sandstone.2", "minecraft:cut_red_sandstone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stonebrick.0", "minecraft:stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stonebrick.1", "minecraft:mossy_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stonebrick.2", "minecraft:cracked_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stonebrick.3", "minecraft:chiseled_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.0", "minecraft:infested_stone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.1", "minecraft:infested_cobblestone");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.2", "minecraft:infested_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.3", "minecraft:infested_mossy_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.4", "minecraft:infested_cracked_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:monster_egg.5", "minecraft:infested_chiseled_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:yellow_flower.0", "minecraft:dandelion");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.0", "minecraft:poppy");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.1", "minecraft:blue_orchid");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.2", "minecraft:allium");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.3", "minecraft:azure_bluet");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.4", "minecraft:red_tulip");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.5", "minecraft:orange_tulip");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.6", "minecraft:white_tulip");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.7", "minecraft:pink_tulip");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_flower.8", "minecraft:oxeye_daisy");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.0", "minecraft:sunflower");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.1", "minecraft:lilac");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.2", "minecraft:tall_grass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.3", "minecraft:large_fern");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.4", "minecraft:rose_bush");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:double_plant.5", "minecraft:peony");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:deadbush.0", "minecraft:dead_bush");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:tallgrass.0", "minecraft:dead_bush");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:tallgrass.1", "minecraft:grass");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:tallgrass.2", "minecraft:fern");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sponge.0", "minecraft:sponge");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:sponge.1", "minecraft:wet_sponge");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:purpur_slab.0", "minecraft:purpur_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.0", "minecraft:stone_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.1", "minecraft:sandstone_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.2", "minecraft:petrified_oak_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.3", "minecraft:cobblestone_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.4", "minecraft:brick_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.5", "minecraft:stone_brick_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.6", "minecraft:nether_brick_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab.7", "minecraft:quartz_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_slab2.0", "minecraft:red_sandstone_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.0", "minecraft:oak_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.1", "minecraft:spruce_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.2", "minecraft:birch_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.3", "minecraft:jungle_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.4", "minecraft:acacia_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_slab.5", "minecraft:dark_oak_slab");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:coal.0", "minecraft:coal");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:coal.1", "minecraft:charcoal");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fish.0", "minecraft:cod");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fish.1", "minecraft:salmon");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fish.2", "minecraft:clownfish");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fish.3", "minecraft:pufferfish");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:cooked_fish.0", "minecraft:cooked_cod");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:cooked_fish.1", "minecraft:cooked_salmon");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.0", "minecraft:skeleton_skull");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.1", "minecraft:wither_skeleton_skull");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.2", "minecraft:zombie_head");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.3", "minecraft:player_head");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.4", "minecraft:creeper_head");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:skull.5", "minecraft:dragon_head");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:golden_apple.0", "minecraft:golden_apple");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:golden_apple.1", "minecraft:enchanted_golden_apple");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fireworks.0", "minecraft:firework_rocket");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:firework_charge.0", "minecraft:firework_star");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.0", "minecraft:ink_sac");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.1", "minecraft:rose_red");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.2", "minecraft:cactus_green");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.3", "minecraft:cocoa_beans");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.4", "minecraft:lapis_lazuli");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.5", "minecraft:purple_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.6", "minecraft:cyan_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.7", "minecraft:light_gray_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.8", "minecraft:gray_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.9", "minecraft:pink_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.10", "minecraft:lime_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.11", "minecraft:dandelion_yellow");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.12", "minecraft:light_blue_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.13", "minecraft:magenta_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.14", "minecraft:orange_dye");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:dye.15", "minecraft:bone_meal");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:silver_shulker_box.0", "minecraft:light_gray_shulker_box");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fence.0", "minecraft:oak_fence");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:fence_gate.0", "minecraft:oak_fence_gate");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_door.0", "minecraft:oak_door");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:boat.0", "minecraft:oak_boat");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:lit_pumpkin.0", "minecraft:jack_o_lantern");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:pumpkin.0", "minecraft:carved_pumpkin");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:trapdoor.0", "minecraft:oak_trapdoor");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:nether_brick.0", "minecraft:nether_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:red_nether_brick.0", "minecraft:red_nether_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:netherbrick.0", "minecraft:nether_brick");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_button.0", "minecraft:oak_button");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:wooden_pressure_plate.0", "minecraft:oak_pressure_plate");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:noteblock.0", "minecraft:note_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.0", "minecraft:white_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.1", "minecraft:orange_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.2", "minecraft:magenta_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.3", "minecraft:light_blue_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.4", "minecraft:yellow_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.5", "minecraft:lime_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.6", "minecraft:pink_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.7", "minecraft:gray_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.8", "minecraft:light_gray_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.9", "minecraft:cyan_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.10", "minecraft:purple_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.11", "minecraft:blue_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.12", "minecraft:brown_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.13", "minecraft:green_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.14", "minecraft:red_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:bed.15", "minecraft:black_bed");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.15", "minecraft:white_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.14", "minecraft:orange_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.13", "minecraft:magenta_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.12", "minecraft:light_blue_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.11", "minecraft:yellow_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.10", "minecraft:lime_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.9", "minecraft:pink_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.8", "minecraft:gray_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.7", "minecraft:light_gray_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.6", "minecraft:cyan_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.5", "minecraft:purple_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.4", "minecraft:blue_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.3", "minecraft:brown_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.2", "minecraft:green_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.1", "minecraft:red_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:banner.0", "minecraft:black_banner");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:grass.0", "minecraft:grass_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:brick_block.0", "minecraft:bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:end_bricks.0", "minecraft:end_stone_bricks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:golden_rail.0", "minecraft:powered_rail");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:magma.0", "minecraft:magma_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:quartz_ore.0", "minecraft:nether_quartz_ore");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:reeds.0", "minecraft:sugar_cane");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:slime.0", "minecraft:slime_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:stone_stairs.0", "minecraft:cobblestone_stairs");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:waterlily.0", "minecraft:lily_pad");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:web.0", "minecraft:cobweb");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:snow.0", "minecraft:snow_block");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:snow_layer.0", "minecraft:snow");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_11.0", "minecraft:music_disc_11");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_13.0", "minecraft:music_disc_13");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_blocks.0", "minecraft:music_disc_blocks");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_cat.0", "minecraft:music_disc_cat");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_chirp.0", "minecraft:music_disc_chirp");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_far.0", "minecraft:music_disc_far");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_mall.0", "minecraft:music_disc_mall");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_mellohi.0", "minecraft:music_disc_mellohi");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_stal.0", "minecraft:music_disc_stal");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_strad.0", "minecraft:music_disc_strad");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_wait.0", "minecraft:music_disc_wait");
        ITEM_WITH_DAMAGE_RENAMES.put("minecraft:record_ward.0", "minecraft:music_disc_ward");
        ITEM_IDS = ITEM_WITH_DAMAGE_RENAMES.keySet().stream()
            .map(name -> name.substring(0, name.indexOf('.')))
            .collect(Collectors.toSet());
        ITEMS_WITH_DURABILITY = Set.of(
            "minecraft:bow",
            "minecraft:carrot_on_a_stick",
            "minecraft:chainmail_boots",
            "minecraft:chainmail_chestplate",
            "minecraft:chainmail_helmet",
            "minecraft:chainmail_leggings",
            "minecraft:diamond_axe",
            "minecraft:diamond_boots",
            "minecraft:diamond_chestplate",
            "minecraft:diamond_helmet",
            "minecraft:diamond_hoe",
            "minecraft:diamond_leggings",
            "minecraft:diamond_pickaxe",
            "minecraft:diamond_shovel",
            "minecraft:diamond_sword",
            "minecraft:elytra",
            "minecraft:fishing_rod",
            "minecraft:flint_and_steel",
            "minecraft:golden_axe",
            "minecraft:golden_boots",
            "minecraft:golden_chestplate",
            "minecraft:golden_helmet",
            "minecraft:golden_hoe",
            "minecraft:golden_leggings",
            "minecraft:golden_pickaxe",
            "minecraft:golden_shovel",
            "minecraft:golden_sword",
            "minecraft:iron_axe",
            "minecraft:iron_boots",
            "minecraft:iron_chestplate",
            "minecraft:iron_helmet",
            "minecraft:iron_hoe",
            "minecraft:iron_leggings",
            "minecraft:iron_pickaxe",
            "minecraft:iron_shovel",
            "minecraft:iron_sword",
            "minecraft:leather_boots",
            "minecraft:leather_chestplate",
            "minecraft:leather_helmet",
            "minecraft:leather_leggings",
            "minecraft:shears",
            "minecraft:shield",
            "minecraft:stone_axe",
            "minecraft:stone_hoe",
            "minecraft:stone_pickaxe",
            "minecraft:stone_shovel",
            "minecraft:stone_sword",
            "minecraft:wooden_axe",
            "minecraft:wooden_hoe",
            "minecraft:wooden_pickaxe",
            "minecraft:wooden_shovel",
            "minecraft:wooden_sword"
        );
    }
}
