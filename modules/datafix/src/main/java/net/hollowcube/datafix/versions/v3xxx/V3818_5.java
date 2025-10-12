package net.hollowcube.datafix.versions.v3xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

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
        int count = value.remove("Count").as(Number.class, 1).intValue();
        value.put("count", count);

        var components = Value.emptyMap();
        var tag = value.remove("tag");

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
        components.put("minecraft:enchantments", fixEnchantments(tag.remove("Enchantments"), components, (hideFlags & 1) != 0));
        if ("minecraft:enchanted_book".equals(id)) components.put("minecraft:stored_enchantments",
                fixEnchantments(tag.remove("StoredEnchantments"), components, (hideFlags & 1) != 0));

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

        components.put("minecraft:can_break", fixBlockStatePredicateList(tag.remove("CanDestroy"), (hideFlags & 8) != 0));
        components.put("minecraft:can_place_on", fixBlockStatePredicateList(tag.remove("CanPlaceOn"), (hideFlags & 16) != 0));

        var attributeModifiers = tag.remove("AttributeModifiers");
        if (attributeModifiers.size(0) > 0) components.put("minecraft:attribute_modifiers",
                fixAttributeModifierList(attributeModifiers, (hideFlags & 2) != 0));

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
            var decorations = fixMapDecorations(tag.remove("Decorations"));
            if (decorations.size(0) > 0)
                components.put("minecraft:map_decorations", decorations);
        }

        if (POTION_ITEMS.contains(id)) {
            components.put("minecraft:potion_contents", fixPotionContents(tag));
        }

        if ("minecraft:writable_book".equals(id)) {
            components.put("minecraft:writable_book_content", fixWritableBook(tag));
        }

        if ("minecraft:written_book".equals(id)) {
            components.put("minecraft:written_book_content", fixWrittenBook(tag));
        }

        if ("minecraft:suspicious_stew".equals(id)) {
            components.put("minecraft:suspicious_stew_effects", tag.remove("effects"));
        }

        if ("minecraft:debug_stick".equals(id)) {
            components.put("minecraft:debug_stick_state", tag.remove("DebugProperty"));
        }

        if (BUCKETED_MOB_IDS.contains(id)) {
            fixBucketedMobData(tag, components);
        }

        if ("minecraft:goat_horn".equals(id)) {
            components.put("minecraft:instrument", tag.remove("instrument"));
        }

        if ("minecraft:knowledge_book".equals(id)) {
            components.put("minecraft:recipes", tag.remove("Recipes"));
        }

        if ("minecraft:compass".equals(id)) {
            components.put("minecraft:lodestone_tracker", fixLodestoneTracker(tag));
        }

        if ("minecraft:firework_rocket".equals(id)) {
            components.put("minecraft:fireworks", fixFireworkRocket(tag.remove("Fireworks")));
        }

        if ("minecraft:firework_star".equals(id)) {
            components.put("minecraft:firework_explosion", fixExplosion(tag.remove("Explosion")));
        }

        if ("minecraft:player_head".equals(id)) {
            var skullOwner = tag.remove("SkullOwner");
            components.put("minecraft:profile", fixProfile(skullOwner));
        }

        if (tag.size(0) > 0)
            components.put("minecraft:custom_data", tag);
        if (components.size(0) > 0)
            value.put("components", components);
        return null;
    }

    private static Value fixBlockStateTag(Value blockStateTag) {
        if (blockStateTag.isNull()) return null;
        var newBlockState = Value.emptyMap();
        blockStateTag.forEachEntry((key, value) -> {
            if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(key) && value.value() instanceof Boolean b) {
                newBlockState.put(key, String.valueOf(b));
            } else if (value.value() instanceof Number n) {
                newBlockState.put(key, String.valueOf(n));
            } else newBlockState.put(key, value);
        });
        return newBlockState;
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
                    container.put(containerItem);
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
                    newItems.put(newItem);
                }
                if (newItems.size(0) > 0)
                    components.put("minecraft:container", newItems);
            }
            case "minecraft:beehive" -> components.put("minecraft:bees", blockEntityTag.remove("bees"));
        }
        return blockEntityTag;
    }

    private static Value fixEnchantments(Value enchantments, Value components, boolean isHidden) {
        if (enchantments.isNull()) return null;
        var newEnchantments = Value.emptyMap();
        var levels = Value.emptyMap();
        for (var enchantment : enchantments) {
            var id = enchantment.get("id").as(String.class, null);
            if (id == null) continue;
            var level = Math.clamp(enchantment.get("lvl").as(Number.class, 0).intValue(), 0, 255);
            if (level == 0) continue;
            levels.put(id, level);
        }
        newEnchantments.put("levels", levels);
        if (isHidden)
            newEnchantments.put("show_in_tooltip", false);
        if (levels.size(0) == 0)
            components.put("minecraft:enchantment_glint_override", true);
        return levels.size(0) > 0 || isHidden ? newEnchantments : null;
    }

    private static Value fixBlockStatePredicateList(Value predicateList, boolean isHidden) {
        if (predicateList.isNull()) return null;
        var newPredicate = Value.emptyMap();
        var newPredicateList = Value.emptyList();
        for (var predicateValue : predicateList) {
            newPredicateList.put(predicateValue.value() instanceof String predicate
                    ? fixBlockStatePredicate(predicate) : predicateValue);
        }
        newPredicate.put("predicate", newPredicateList);
        if (isHidden) newPredicate.put("show_in_tooltip", false);
        return newPredicate;
    }

    private static Value fixBlockStatePredicate(@NotNull String predicate) {
        int openBracket = predicate.indexOf('['), closeBracket = predicate.indexOf(']');
        int openBrace = predicate.indexOf('{'), closeBrace = predicate.indexOf('}');
        int nameLength = openBracket != -1 ? openBracket : predicate.length();
        if (openBrace != -1) nameLength = Math.min(nameLength, openBrace);

        Value result = Value.emptyMap();
        result.put("blocks", predicate.substring(0, nameLength).trim());

        if (openBracket != -1 && closeBracket != -1) {
            var properties = Value.emptyMap();
            for (String propertyKeyValue : predicate.substring(openBracket + 1, closeBracket).split(",")) {
                int equals = propertyKeyValue.indexOf('=');
                if (equals == -1) continue;
                properties.put(
                        propertyKeyValue.substring(0, equals).trim().intern(),
                        propertyKeyValue.substring(equals + 1).trim().intern()
                );
            }
            result.put("state", properties);
        }

        if (openBrace != -1 && closeBrace != -1) {
            result.put("nbt", predicate.substring(openBrace, closeBrace + 1));
        }

        return result;
    }

    private static Value fixAttributeModifierList(@NotNull Value attributeModifiers, boolean isHidden) {
        var newAttributeModifiers = Value.emptyMap();

        var modifierList = Value.emptyList();
        for (var modifier : attributeModifiers) {
            modifierList.put(fixAttributeModifier(modifier));
        }
        newAttributeModifiers.put("modifiers", modifierList);

        if (isHidden) newAttributeModifiers.put("show_in_tooltip", false);
        return newAttributeModifiers;
    }

    private static Value fixAttributeModifier(@NotNull Value modifier) {
        var newModifier = Value.emptyMap();
        newModifier.put("type", modifier.get("AttributeName"));
        newModifier.put("slot", modifier.get("Slot"));
        newModifier.put("uuid", modifier.get("UUID"));
        newModifier.put("name", modifier.get("Name", () -> Value.wrap("")));
        newModifier.put("amount", modifier.get("Amount", () -> Value.wrap(0.0)));
        newModifier.put("operation", switch (modifier.get("Operation").as(Number.class, 0).intValue()) {
            case 0 -> "add_multiplied_base";
            case 1 -> "add_multiplied_total";
            default -> "add_value";
        });
        return newModifier;
    }

    private static void fixBucketedMobData(@NotNull Value tag, @NotNull Value components) {
        var bucketEntityData = Value.emptyMap();
        for (var key : BUCKETED_MOB_TAGS)
            bucketEntityData.put(key, tag.remove(key));
        if (bucketEntityData.size(0) > 0)
            components.put("minecraft:bucket_entity_data", bucketEntityData);
    }

    private static Value fixLodestoneTracker(Value tag) {
        var lodestonePos = tag.remove("LodestonePos");
        var lodestoneDimension = tag.remove("LodestoneDimension");
        if (lodestonePos.isNull() && lodestoneDimension.isNull()) return null;

        var lodestoneTracker = Value.emptyMap();
        if (!lodestonePos.isNull() && !lodestoneDimension.isNull()) {
            var target = Value.emptyMap();
            target.put("pos", lodestonePos);
            target.put("dimension", lodestoneDimension);
            lodestoneTracker.put("target", target);
        }
        if (!tag.remove("LodestoneTracked").as(Boolean.class, true)) {
            lodestoneTracker.put("tracked", false);
        }
        return lodestoneTracker;
    }

    private static Value fixFireworkRocket(Value fireworks) {
        if (fireworks.isNull()) return null;

        var newFireworks = Value.emptyMap();
        var newExplosions = Value.emptyList();
        for (var explosion : fireworks.get("Explosions")) {
            newExplosions.put(fixExplosion(explosion));
        }
        newFireworks.put("explosions", newExplosions);
        int flightDuration = fireworks.remove("Flight").as(Number.class, 0).intValue();
        newFireworks.put("flight_duration", flightDuration);
        return newFireworks;
    }

    private static Value fixExplosion(@NotNull Value explosion) {
        var type = explosion.remove("Type").as(Number.class, 0).intValue();
        explosion.put("shape", switch (type) {
            case 1 -> "large_ball";
            case 2 -> "star";
            case 3 -> "creeper";
            case 4 -> "burst";
            default -> "small_ball";
        });
        explosion.put("colors", explosion.remove("Colors"));
        explosion.put("fade_colors", explosion.remove("FadeColors"));
        explosion.put("has_trail", explosion.remove("Trail"));
        explosion.put("has_twinkle", explosion.remove("Flicker"));
        return explosion;
    }

    private static Value fixPotionContents(@NotNull Value tag) {
        var potionContents = Value.emptyMap();
        if (tag.remove("Potion").value() instanceof String s && !"minecraft:empty".equals(s))
            potionContents.put("potion", s);
        potionContents.put("custom_color", tag.remove("CustomPotionColor"));
        potionContents.put("custom_effects", tag.remove("custom_potion_effects"));
        return potionContents.size(0) > 0 ? potionContents : null;
    }

    private static Value fixMapDecorations(@NotNull Value decorationList) {
        var mapDecorations = Value.emptyMap();
        for (var mapDecoration : decorationList) {
            var key = mapDecoration.get("id").as(String.class, "");
            var value = Value.emptyMap();
            value.put("type", MAP_DECORATION_IDS.get(mapDecoration.get("type").as(Number.class, 0).intValue()));
            value.put("x", mapDecoration.get("x").as(Number.class, 0.0).doubleValue());
            value.put("z", mapDecoration.get("z").as(Number.class, 0.0).doubleValue());
            value.put("rotation", mapDecoration.get("rot").as(Number.class, 0f).floatValue());
            mapDecorations.put(key, value);
        }
        return mapDecorations;
    }

    static Value fixProfile(Value skullOwner) {
        if (skullOwner.isNull()) return null;
        if (skullOwner.value() instanceof String name) {
            var result = Value.emptyMap();
            if (isValidPlayerName(name)) result.put("name", name);
            return result;
        }
        if (!skullOwner.isMapLike()) return null;

        var result = Value.emptyMap();
        var name = skullOwner.get("Name").as(String.class, null);
        if (isValidPlayerName(name)) result.put("name", name);
        result.put("id", skullOwner.get("Id"));
        result.put("properties", fixProfileProperties(skullOwner.get("Properties")));
        return result;
    }

    private static Value fixProfileProperties(Value properties) {
        if (properties.isNull()) return null;
        var result = Value.emptyList();
        properties.forEachEntry((key, value) -> {
            for (var property : value) {
                var entry = Value.emptyMap();
                entry.put("name", key);
                entry.put("value", property.get("Value"));
                entry.put("signature", property.get("Signature"));
                result.put(entry);
            }
        });
        return result.size(0) > 0 ? result : null;
    }

    private static boolean isValidPlayerName(String string) {
        return string != null && string.length() <= 16 && string.chars().filter(i -> i <= 32 || i >= 127).findAny().isEmpty();
    }

    private static Value fixWritableBook(Value tag) {
        var pages = fixBookPages(tag);
        if (pages.size(0) == 0) return null;
        var result = Value.emptyMap();
        result.put("pages", result);
        return result;
    }

    private static Value fixWrittenBook(Value tag) {
        var result = Value.emptyMap();
        var pages = fixBookPages(tag);
        if (pages.size(0) > 0) result.put("pages", pages);
        result.put("title", createFilteredText(tag.remove("title"), tag.remove("filtered_title")));
        result.put("author", tag.remove("author"));
        result.put("resolved", tag.remove("resolved"));
        result.put("generation", tag.remove("generation"));
        return result;
    }

    private static Value fixBookPages(Value tag) {
        var result = Value.emptyList();
        var pages = tag.remove("pages");
        var filteredPages = tag.remove("filtered_pages");
        for (int i = 0; i < pages.size(0); i++) {
            var page = pages.get(i);
            if (page.isNull()) continue;
            result.put(createFilteredText(page, filteredPages.get(String.valueOf(i))));
        }
        return result;
    }

    private static Value createFilteredText(Value raw, Value filtered) {
        var result = Value.emptyMap();
        result.put("raw", raw);
        if (!filtered.isNull()) result.put("filtered", filtered);
        return result;
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
