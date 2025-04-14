package net.hollowcube.datafix.versions.v3xxx;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V3568 extends DataVersion {
    private static final Int2ObjectMap<String> POTION_IDS = new Int2ObjectArrayMap<>();
    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion", "minecraft:splash_potion",
            "minecraft:lingering_potion", "minecraft:tipped_arrow"
    );

    public V3568() {
        super(3568);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:beacon", V3568::fixBeaconBlockEntity);

        addFix(DataTypes.ITEM_STACK, "minecraft:suspicious_stew", V3568::fixSuspiciousStewItemStack);
        POTION_ITEMS.forEach(id -> addFix(DataTypes.ITEM_STACK, id, V3568::fixPotionlikeItemStack));

        addFix(DataTypes.ENTITY, "minecraft:mooshroom", V3568::fixMooshroomEntity);
        addFix(DataTypes.ENTITY, "minecraft:arrow", V3568::fixArrowEntity);
        addFix(DataTypes.ENTITY, "minecraft:area_effect_cloud", V3568::fixAreaEffectCloudEntity);
        addFix(DataTypes.ENTITY, V3568::fixLivingEntity);
    }

    private static Value fixBeaconBlockEntity(Value value) {
        fixEffectIdAndRename(value, "Primary", "primary_effect");
        fixEffectIdAndRename(value, "Secondary", "secondary_effect");
        return null;
    }

    private static Value fixSuspiciousStewItemStack(Value itemStack) {
        var tag = itemStack.get("tag");
        if (!tag.isMapLike()) return null;

        fixEffectList(tag, "Effects", "effects");
        return null;
    }

    private static Value fixPotionlikeItemStack(Value itemStack) {
        var tag = itemStack.get("tag");
        if (!tag.isMapLike()) return null;

        fixEffectList(tag, "CustomPotionEffects", "custom_potion_effects");
        return null;
    }

    private static Value fixMooshroomEntity(Value entity) {
        var effect = Value.emptyMap();
        effect.put("id", entity.remove("EffectId"));
        fixEffectId(effect, "id");
        effect.put("duration", entity.remove("EffectDuration"));

        if (effect.equals(Value.emptyMap())) return null;
        var stewEffects = Value.emptyList();
        stewEffects.add(effect);
        entity.put("stew_effects", stewEffects);

        return null;
    }

    private static Value fixArrowEntity(Value entity) {
        fixEffectList(entity, "CustomPotionEffects", "custom_potion_effects");
        return null;
    }

    private static Value fixAreaEffectCloudEntity(Value entity) {
        fixEffectList(entity, "Effects", "effects");
        return null;
    }

    private static Value fixLivingEntity(Value entity) {
        fixEffectList(entity, "ActiveEffects", "active_effects");
        return null;
    }

    private static void fixEffectIdAndRename(Value value, String fromField, String toField) {
        fixEffectId(value, fromField);
        value.put(toField, value.remove(fromField));
    }

    private static void fixEffectList(Value value, String fromField, String toField) {
        for (var effect : value.get(fromField)) fixEffect(effect);
        value.put(toField, value.remove(fromField));
    }

    private static void fixEffect(Value effect) {
        fixEffectIdAndRename(effect, "Id", "id");
        var hiddenEffect = effect.get("HiddenEffect");
        if (!hiddenEffect.isNull()) fixEffect(hiddenEffect);
        effect.put("ambient", effect.remove("Ambient"));
        effect.put("amplifier", effect.remove("Amplifier"));
        effect.put("duration", effect.remove("Duration"));
        effect.put("show_particles", effect.remove("ShowParticles"));
        effect.put("show_icon", effect.remove("ShowIcon"));
        effect.put("hidden_effect", effect.remove("HiddenEffect"));
    }

    private static void fixEffectId(Value value, String field) {
        var idNumber = value.get(field).as(Number.class, null);
        if (idNumber == null) return;
        value.put(field, POTION_IDS.get(idNumber.intValue()));
    }

    static {
        POTION_IDS.put(1, "minecraft:speed");
        POTION_IDS.put(2, "minecraft:slowness");
        POTION_IDS.put(3, "minecraft:haste");
        POTION_IDS.put(4, "minecraft:mining_fatigue");
        POTION_IDS.put(5, "minecraft:strength");
        POTION_IDS.put(6, "minecraft:instant_health");
        POTION_IDS.put(7, "minecraft:instant_damage");
        POTION_IDS.put(8, "minecraft:jump_boost");
        POTION_IDS.put(9, "minecraft:nausea");
        POTION_IDS.put(10, "minecraft:regeneration");
        POTION_IDS.put(11, "minecraft:resistance");
        POTION_IDS.put(12, "minecraft:fire_resistance");
        POTION_IDS.put(13, "minecraft:water_breathing");
        POTION_IDS.put(14, "minecraft:invisibility");
        POTION_IDS.put(15, "minecraft:blindness");
        POTION_IDS.put(16, "minecraft:night_vision");
        POTION_IDS.put(17, "minecraft:hunger");
        POTION_IDS.put(18, "minecraft:weakness");
        POTION_IDS.put(19, "minecraft:poison");
        POTION_IDS.put(20, "minecraft:wither");
        POTION_IDS.put(21, "minecraft:health_boost");
        POTION_IDS.put(22, "minecraft:absorption");
        POTION_IDS.put(23, "minecraft:saturation");
        POTION_IDS.put(24, "minecraft:glowing");
        POTION_IDS.put(25, "minecraft:levitation");
        POTION_IDS.put(26, "minecraft:luck");
        POTION_IDS.put(27, "minecraft:unluck");
        POTION_IDS.put(28, "minecraft:slow_falling");
        POTION_IDS.put(29, "minecraft:conduit_power");
        POTION_IDS.put(30, "minecraft:dolphins_grace");
        POTION_IDS.put(31, "minecraft:bad_omen");
        POTION_IDS.put(32, "minecraft:hero_of_the_village");
        POTION_IDS.put(33, "minecraft:darkness");
    }
}
