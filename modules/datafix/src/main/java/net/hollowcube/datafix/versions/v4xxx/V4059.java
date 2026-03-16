package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4059 extends DataVersion {
    public V4059() {
        super(4059);

        addReference(DataTypes.DATA_COMPONENTS, field -> field
                         // todo we should be removing food here
                         .single("minecraft:use_remainder", DataTypes.ITEM_STACK)
                // TODO equippable here.
        );

        addFix(DataTypes.DATA_COMPONENTS, V4059::fixFoodToConsumableComponent);
    }

    private static @Nullable Value fixFoodToConsumableComponent(Value dataComponents) {
        var food = dataComponents.get("minecraft:food");
        if (!food.isMapLike()) return null;

        var consumable = Value.emptyMap();

        float consumeSeconds = food.remove("eat_seconds").as(Number.class, 1.6f).floatValue();
        consumable.put("consume_seconds", consumeSeconds);

        dataComponents.put("minecraft:use_remainder", food.remove("using_converts_to"));

        var effects = Value.emptyList();
        for (var effect : food.get("effects")) {
            var effectData = Value.emptyMap();
            effectData.put("type", "minecraft:apply_effects");
            effectData.put("effects", effect.get("effect"));
            effectData.put("probability", effect.get("probability").as(Number.class, 1.0f).floatValue());
            effects.put(effect);
        }
        consumable.put("on_consume_effects", effects);

        dataComponents.put("minecraft:consumable", consumable);
        return null;
    }

}
