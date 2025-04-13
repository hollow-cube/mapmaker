package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V3322 extends DataVersion {
    private static final Set<String> POTION_ITEMS = Set.of(
            "minecraft:potion", "minecraft:splash_potion",
            "minecraft:lingering_potion", "minecraft:tipped_arrow"
    );

    public V3322() {
        super(3322);

        addFix(DataType.ENTITY, V3322::fixEntityEffects);
        POTION_ITEMS.forEach(id -> addFix(DataType.ITEM_STACK, id, V3322::fixItemEffects));
    }

    private static Value fixEntityEffects(Value entity) {
        fixEffectList(entity.get("Effects"));
        fixEffectList(entity.get("ActiveEffects"));
        fixEffectList(entity.get("CustomPotionEffects"));
        return null;
    }

    private static Value fixItemEffects(Value itemStack) {
        fixEffectList(itemStack.get("tag").get("CustomPotionEffects"));
        return null;
    }

    private static void fixEffectList(Value effectList) {
        for (var effect : effectList) {
            var factorCalculationData = effect.get("FactorCalculationData");
            int changedTimestamp = factorCalculationData.remove("effect_changed_timestamp")
                    .as(Number.class, -1).intValue();
            int duration = effect.get("Duration").as(Number.class, -1).intValue();
            effect.put("ticks_active", changedTimestamp - duration);
        }
    }
}
