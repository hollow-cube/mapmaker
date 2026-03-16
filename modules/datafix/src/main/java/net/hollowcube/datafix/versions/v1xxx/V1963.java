package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1963 extends DataVersion {
    public V1963() {
        super(0);

        addFix(DataTypes.ENTITY, "minecraft:villager", V1963::fixRemoveGolemGossip);
    }

    private static @Nullable Value fixRemoveGolemGossip(Value entity) {
        var gossips = entity.get("Gossips");
        if (gossips.isNull() || gossips.size(0) == 0)
            return null;

        var newGossips = Value.emptyList();
        for (var gossip : gossips) {
            if ("golem".equals(gossip.get("Type").as(String.class, "")))
                continue;
            newGossips.put(gossip);
        }

        entity.put("Gossips", newGossips);
        return null;
    }
}
