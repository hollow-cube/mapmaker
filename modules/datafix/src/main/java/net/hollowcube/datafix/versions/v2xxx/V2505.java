package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V2505 extends DataVersion {
    public V2505() {
        super(2505);

        addReference(DataTypes.ENTITY, "minecraft:piglin", field -> field
                .list("Inventory", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "minecraft:villager", V2505::fixMemoryExpiry);
    }

    private static Value fixMemoryExpiry(Value entity) {
        var memories = entity.get("Brain").get("memories");
        if (!memories.isMapLike()) return null;

        var newMemories = Value.emptyMap();
        memories.forEachEntry((key, value) -> {
            var newValue = Value.emptyMap();
            newValue.put("value", value);
            newMemories.put(key, newValue);
        });
        entity.get("Brain").put("memories", newMemories);

        return null;
    }
}
