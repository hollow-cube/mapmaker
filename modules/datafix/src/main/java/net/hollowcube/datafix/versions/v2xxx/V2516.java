package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.UUIDFixes;
import net.hollowcube.datafix.util.Value;

public class V2516 extends DataVersion {
    public V2516() {
        super(2516);

        addFix(DataType.ENTITY, "minecraft:villager", V2516::fixEntityGossipUuid);
        addFix(DataType.ENTITY, "minecraft:zombie_villager", V2516::fixEntityGossipUuid);
    }

    private static Value fixEntityGossipUuid(Value entity) {
        for (var gossip : entity.get("Gossips"))
            UUIDFixes.replaceUuidFromLeastMost(gossip, "Target", "Target");
        return null;
    }
}
