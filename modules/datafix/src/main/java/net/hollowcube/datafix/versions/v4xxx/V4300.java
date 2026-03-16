package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class V4300 extends DataVersion {
    private static final Set<String> ENTITIES_WITH_SADDLE_ITEM = Set.of(
        "minecraft:horse",
        "minecraft:skeleton_horse",
        "minecraft:zombie_horse",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:camel",
        "minecraft:llama",
        "minecraft:trader_llama"
    );
    private static final Set<String> ENTITIES_WITH_SADDLE_FLAG = Set.of("minecraft:pig", "minecraft:strider");

    public V4300() {
        super(4300);

        addReference(DataTypes.ENTITY, "minecraft:llama", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:trader_llama", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:donkey", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:mule", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:horse");
        addReference(DataTypes.ENTITY, "minecraft:skeleton_horse");
        addReference(DataTypes.ENTITY, "minecraft:zombie_horse");

        ENTITIES_WITH_SADDLE_ITEM.forEach(id -> addFix(DataTypes.ENTITY, id, V4300::fixEntityWithSaddleItem));
        ENTITIES_WITH_SADDLE_FLAG.forEach(id -> addFix(DataTypes.ENTITY, id, V4300::fixEntityWithSaddleFlag));
    }

    static DataType.Builder entityWithInventory(DataType.Builder field) {
        return field.list("Items", DataTypes.ITEM_STACK);
    }

    private static @Nullable Value fixEntityWithSaddleItem(Value entity) {
        var saddleItem = entity.remove("SaddleItem");
        if (saddleItem.isNull()) return null;
        entity.put("saddle", saddleItem);
        entity.get("drop_chances", Value::emptyMap).put("saddle", 2.0f);
        return null;
    }

    private static @Nullable Value fixEntityWithSaddleFlag(Value entity) {
        var hasSaddle = entity.remove("Saddle").as(Boolean.class, false);
        if (!hasSaddle) return null;

        var saddleItem = Value.emptyMap();
        saddleItem.put("id", "minecraft:saddle");
        saddleItem.put("count", 1);
        entity.put("saddle", saddleItem);

        entity.get("drop_chances", Value::emptyMap).put("saddle", 2.0f);
        return null;
    }

}
