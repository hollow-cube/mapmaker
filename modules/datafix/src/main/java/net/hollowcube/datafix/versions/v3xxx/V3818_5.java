package net.hollowcube.datafix.versions.v3xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;

import java.util.Objects;
import java.util.Set;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class V3818_5 extends DataVersion {
    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion", "minecraft:splash_potion",
            "minecraft:lingering_potion", "minecraft:tipped_arrow"
    );
    private static final Set<String> BUCKETED_MOB_IDS = Set.of(
            "minecraft:pufferfish_bucket", "minecraft:salmon_bucket",
            "minecraft:cod_bucket", "minecraft:tropical_fish_bucket",
            "minecraft:axolotl_bucket", "minecraft:tadpole_bucket"
    );
    private static final Set<String> BUCKETED_MOB_TAGS = Set.of(
            "NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable",
            "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
    );
    private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES;
    private static final Int2ObjectMap<String> MAP_DECORATION_IDS;

    public V3818_5() {
        super(3818, 5);

        addReference(DataTypes.ITEM_STACK, field -> field
                .single("components", DataTypes.DATA_COMPONENTS));

        addFix(DataTypes.ITEM_STACK, V3818_5::fixItemStack);
    }

    private static Value fixItemStack(Value value) {
        String id = namespaced(value.get("id").as(String.class, null));
        if (id == null) return null;
        int count = value.remove("Count").as(Integer.class, 1);
        value.put("count", count);

        var components = Value.emptyMap();
        var tag = value.get("tag");

        int hideFlags = tag.remove("HideFlags").as(Number.class, 0).intValue();
        int damage = tag.remove("Damage").as(Integer.class, 0);
        if (damage != 0) components.put("minecraft:damage", damage);
        int repairCost = tag.remove("RepairCost").as(Integer.class, 0);
        if (repairCost != 0) components.put("minecraft:repair_cost", repairCost);
        components.put("minecraft:custom_model_data", tag.remove("CustomModelData"));
        components.put("minecraft:block_state", fixBlockStateTag(tag.remove("BlockStateTag")));
        components.put("minecraft:entity_data", tag.remove("EntityTag"));
        components.put("minecraft:block_entity_data", fixBlockEntityTag(id, components, tag.remove("BlockEntityTag")));
        if (tag.remove("Unbreakable").as(Boolean.class, false)) {
            var unbreakable = Value.emptyMap();
            if ((hideFlags & 4) != 0) unbreakable.put("show_in_tooltip", false);
            components.put("minecraft:unbreakable", unbreakable);
        }
        components.put("minecraft:enchantments", fixEnchantments(tag.remove("Enchantments"), (hideFlags & 1) != 0));
        if ("minecraft:enchanted_book".equals(id)) components.put("minecraft:stored_enchantments",
                fixEnchantments(tag.remove("StoredEnchantments"), (hideFlags & 1) != 0));

        var display = tag.remove("display");
        components.put("minecraft:custom_name", display.remove("Name"));
        components.put("minecraft:lore", display.remove("Lore"));
        var color = display.remove("color").as(Number.class, null);
        if (color != null || (hideFlags & 64) != 0) {
            var dyedColor = Value.emptyMap();
            dyedColor.put("rgb", Objects.requireNonNullElse(color, 10511680).intValue());
            if ((hideFlags & 64) != 0) dyedColor.put("show_in_tooltip", false);
            components.put("minecraft:dyed_color", dyedColor);
        }
        var locName = display.remove("LocName").as(String.class, null);
        if (locName != null) components.put("minecraft:item_name", "{\"translate\":\"" + locName + "\"}");
        if ("minecraft:filled_map".equals(id)) components.put("minecraft:map_color", display.remove("MapColor"));

        components.put("minecraft:can_break", fixBlockStatePredicate(tag.remove("CanDestroy"), (hideFlags & 8) != 0));
        components.put("minecraft:can_place_on", fixBlockStatePredicate(tag.remove("CanPlaceOn"), (hideFlags & 16) != 0));

        var trim = tag.remove("Trim");
        if (!trim.isNull()) {
            if ((hideFlags & 128) != 0) trim.put("show_in_tooltip", false);
            components.put("minecraft:trim", trim);
        }

        if ((hideFlags & 32) != 0) components.put("minecraft:hide_additional_tooltip", Value.emptyMap());

        if ("minecraft:crossbow".equals(id)) {
            tag.remove("Charged");
            var chargedProjectiles = tag.remove("ChargedProjectiles");
            if (chargedProjectiles.size(0) > 0)
                components.put("minecraft:charged_projectiles", chargedProjectiles);
        }

        if ("minecraft:bundle".equals(id)) {
            var items = tag.get("Items");
            if (items.size(0) > 0) components.put("minecraft:bundle_contents", items);
        }

        if ("minecraft:filled_map".equals(id)) {
            components.put("minecraft:map_id", tag.remove("map"));
            var decorations = tag.remove("Decorations");
            // todo fixMapDecoration
            if (decorations.size(0) > 0)
                components.put("minecraft:map_decorations", decorations);
        }

        if (POTION_ITEMS.contains(id)) {
            // TODO: fixPotionContents
        }

        if ("minecraft:writable_book".equals(id)) {
            // TODO: fixWritableBook
        }

        if ("minecraft:written_book".equals(id)) {
            // TODO: fixWrittenBook
        }

        if ("minecraft:suspicious_stew".equals(id)) {
            components.put("minecraft:suspicious_stew_effects", tag.remove("effects"));
        }

        if ("minecraft:debug_stick".equals(id)) {
            components.put("minecraft:debug_stick_state", tag.remove("DebugProperty"));
        }

        if (BUCKETED_MOB_IDS.contains(id)) {
            // TODO: fixBucketedMobData
        }

        if ("minecraft:goat_horn".equals(id)) {
            components.put("minecraft:instrument", tag.remove("instrument"));
        }

        if ("minecraft:knowledge_book".equals(id)) {
            components.put("minecraft:recipes", tag.remove("Recipes"));
        }

        if ("minecraft:compass".equals(id)) {
            // TODO: fix compass
        }

        if ("minecraft:firework_rocket".equals(id)) {
            // TODO: fix firework_rocket
        }

        if ("minecraft:firework_star".equals(id)) {
            // TODO: fix firework_star
        }

        if ("minecraft:player_head".equals(id)) {
            var skullOwner = tag.remove("SkullOwner");
            // TODO: fix skull owner
//            components.put("minecraft:profile", );
        }

        if (tag.size(0) > 0)
            components.put("minecraft:custom_data", tag);
        if (components.size(0) > 0)
            value.put("components", components);
        return null;
    }

    private static Value fixBlockStateTag(Value blockStateTag) {
        if (blockStateTag.isNull()) return null;
    }

    private static Value fixBlockEntityTag(String itemId, Value components, Value blockEntityTag) {
        if (blockEntityTag.isNull()) return null;
        components.put("minecraft:lock", blockEntityTag.remove("Lock"));
        var lootTable = blockEntityTag.remove("LootTable");
        if (!lootTable.isNull()) {
            var containerLoot = Value.emptyMap();
            containerLoot.put("loot_table", lootTable);
            long seed = blockEntityTag.remove("LootTableSeed").as(Number.class, 0L).longValue();
            if (seed != 0L) containerLoot.put("seed", seed);
            components.put("minecraft:container_loot", containerLoot);
        }

        switch (itemId) {
            case "minecraft:skull" -> components.put("minecraft:note_block_sound",
                    blockEntityTag.remove("note_block_sound"));
            case "minecraft:decorated_pot" -> {
                components.put("minecraft:pot_decorations", blockEntityTag.remove("sherds"));
                var item = blockEntityTag.remove("item");
                if (!item.isNull()) {
                    var container = Value.emptyList();
                    var containerItem = Value.emptyList();
                    containerItem.put("slot", 0);
                    containerItem.put("item", item);
                    container.add(containerItem);
                    components.put("minecraft:container", container);
                }
            }
            case "minecraft:banner" -> {
                components.put("minecraft:banner_patterns", blockEntityTag.remove("patterns"));
                var base = blockEntityTag.remove("Base").as(Number.class, null);
                if (base != null)
                    components.put("minecraft:base_color", DataFixUtils.dyeColorIdToName(base.intValue()));
            }
            case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace",
                 "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand",
                 "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace",
                 "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
                var newItems = Value.emptyList();
                for (var item : blockEntityTag.get("Items")) {
                    var newItem = Value.emptyMap();
                    var slot = item.remove("Slot").as(Number.class, 0).intValue() & 0xFF;
                    newItem.put("slot", slot);
                    newItem.put("item", item);
                    newItems.add(newItem);
                }
                if (newItems.size(0) > 0)
                    components.put("minecraft:container", newItems);
            }
            case "minecraft:beehive" -> components.put("minecraft:bees", blockEntityTag.remove("bees"));
        }
        return blockEntityTag;
    }

    private static Value fixEnchantments(Value enchantments, boolean isHidden) {
        if (enchantments.isNull()) return null;

    }

    private static Value fixBlockStatePredicate(Value predicateList, boolean isHidden) {
        if (predicateList.isNull()) return null;
        var newPredicate = Value.emptyMap();
        var newPredicateList = Value.emptyList();
        for (var predicate : predicateList) {
            // TODO: fix individual predicates
        }
        newPredicate.put("predicate", newPredicateList);
        if (isHidden) newPredicate.put("show_in_tooltip", false);
        return newPredicate;
    }

    static {
        BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
                "attached",
                "bottom",
                "conditional",
                "disarmed",
                "drag",
                "enabled",
                "extended",
                "eye",
                "falling",
                "hanging",
                "has_bottle_0",
                "has_bottle_1",
                "has_bottle_2",
                "has_record",
                "has_book",
                "inverted",
                "in_wall",
                "lit",
                "locked",
                "occupied",
                "open",
                "persistent",
                "powered",
                "short",
                "signal_fire",
                "snowy",
                "triggered",
                "unstable",
                "waterlogged",
                "berries",
                "bloom",
                "shrieking",
                "can_summon",
                "up",
                "down",
                "north",
                "east",
                "south",
                "west",
                "slot_0_occupied",
                "slot_1_occupied",
                "slot_2_occupied",
                "slot_3_occupied",
                "slot_4_occupied",
                "slot_5_occupied",
                "cracked",
                "crafting"
        );
        MAP_DECORATION_IDS = new Int2ObjectOpenHashMap<>();
        MAP_DECORATION_IDS.defaultReturnValue("player");
        MAP_DECORATION_IDS.put(1, "frame");
        MAP_DECORATION_IDS.put(2, "red_marker");
        MAP_DECORATION_IDS.put(3, "blue_marker");
        MAP_DECORATION_IDS.put(4, "target_x");
        MAP_DECORATION_IDS.put(5, "target_point");
        MAP_DECORATION_IDS.put(6, "player_off_map");
        MAP_DECORATION_IDS.put(7, "player_off_limits");
        MAP_DECORATION_IDS.put(8, "mansion");
        MAP_DECORATION_IDS.put(9, "monument");
        MAP_DECORATION_IDS.put(10, "banner_white");
        MAP_DECORATION_IDS.put(11, "banner_orange");
        MAP_DECORATION_IDS.put(12, "banner_magenta");
        MAP_DECORATION_IDS.put(13, "banner_light_blue");
        MAP_DECORATION_IDS.put(14, "banner_yellow");
        MAP_DECORATION_IDS.put(15, "banner_lime");
        MAP_DECORATION_IDS.put(16, "banner_pink");
        MAP_DECORATION_IDS.put(17, "banner_gray");
        MAP_DECORATION_IDS.put(18, "banner_light_gray");
        MAP_DECORATION_IDS.put(19, "banner_cyan");
        MAP_DECORATION_IDS.put(20, "banner_purple");
        MAP_DECORATION_IDS.put(21, "banner_blue");
        MAP_DECORATION_IDS.put(22, "banner_brown");
        MAP_DECORATION_IDS.put(23, "banner_green");
        MAP_DECORATION_IDS.put(24, "banner_red");
        MAP_DECORATION_IDS.put(25, "banner_black");
        MAP_DECORATION_IDS.put(26, "red_x");
        MAP_DECORATION_IDS.put(27, "village_desert");
        MAP_DECORATION_IDS.put(28, "village_plains");
        MAP_DECORATION_IDS.put(29, "village_savanna");
        MAP_DECORATION_IDS.put(30, "village_snowy");
        MAP_DECORATION_IDS.put(31, "village_taiga");
        MAP_DECORATION_IDS.put(32, "jungle_temple");
        MAP_DECORATION_IDS.put(33, "swamp_hut");
    }

}
