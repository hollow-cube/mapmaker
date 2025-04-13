package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1963 extends DataVersion {
    public V1963() {
        super(0);

        addFix(DataType.ENTITY, "minecraft:villager", V1963::fixRemoveGolemGossip);
    }

    private static Value fixRemoveGolemGossip(Value entity) {
        var gossips = entity.get("Gossips");
        if (gossips.isNull() || gossips.size(0) == 0)
            return null;

        var newGossips = Value.emptyList();
        for (var gossip : gossips) {
            if ("golem".equals(gossip.get("Type").as(String.class, "")))
                continue;
            newGossips.add(gossip);
        }

        entity.put("Gossips", newGossips);
        return null;
    }
}
