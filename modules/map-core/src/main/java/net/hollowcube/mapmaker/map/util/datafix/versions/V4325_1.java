package net.hollowcube.mapmaker.map.util.datafix.versions;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4325_1 extends DataVersion {
    private static final Int2ObjectMap<String> CMD_TO_MODEL_ID = new Int2ObjectArrayMap<>();

    public V4325_1() {
        super(4325, 1);

        addFix(DataTypes.ITEM_STACK, "minecraft:stick", V4325_1::fixFormerHardcodedStickCustomModelData);
        addFix(DataTypes.ITEM_STACK, "minecraft:leather_horse_armor", V4325_1::fixOtherCustomModelData);
    }

    private static Value fixFormerHardcodedStickCustomModelData(Value itemStack) {
        var components = itemStack.get("components");
        var customModelData = components.get("minecraft:custom_model_data");
        var floats = customModelData.get("floats");
        if (customModelData.size(0) == 1 && floats.size(0) == 1) {
            components.remove("minecraft:custom_model_data");
            itemStack.remove("custom_model_data");

            var legacyCmd = floats.get(0).as(Number.class, 0).intValue();
            var modelId = CMD_TO_MODEL_ID.get(legacyCmd);
            if (modelId != null) components.put("minecraft:item_model", modelId);
        }
        return null;
    }

    private static Value fixOtherCustomModelData(Value itemStack) {
        if (!(itemStack.remove("custom_model_data").value() instanceof String s))
            return null;

        itemStack.get("components", Value::emptyMap).put("minecraft:item_model", s);
        return null;
    }

    static {
        CMD_TO_MODEL_ID.put(4, "lb_screen");
        CMD_TO_MODEL_ID.put(5, "sm_house");
        CMD_TO_MODEL_ID.put(6, "train_middle");
        CMD_TO_MODEL_ID.put(7, "train_front");
        CMD_TO_MODEL_ID.put(8, "lb_screen_1x");
        CMD_TO_MODEL_ID.put(12, "flipper");
        CMD_TO_MODEL_ID.put(18, "the_creature");
        CMD_TO_MODEL_ID.put(19, "the_creature");
    }

}
