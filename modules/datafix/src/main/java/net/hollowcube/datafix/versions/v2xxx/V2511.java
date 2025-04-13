package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.UUIDFixes;
import net.hollowcube.datafix.util.Value;

public class V2511 extends DataVersion {
    public V2511() {
        super(2511);

        addFix(DataType.ENTITY, "minecraft:egg", V2511::fixOwnerThrowable);
        addFix(DataType.ENTITY, "minecraft:ender_pearl", V2511::fixOwnerThrowable);
        addFix(DataType.ENTITY, "minecraft:experience_bottle", V2511::fixOwnerThrowable);
        addFix(DataType.ENTITY, "minecraft:snowball", V2511::fixOwnerThrowable);
        addFix(DataType.ENTITY, "minecraft:potion", V2511::fixOwnerThrowable);
        addFix(DataType.ENTITY, "minecraft:llama_spit", V2511::fixOwnerLlamaSpit);
        addFix(DataType.ENTITY, "minecraft:arrow", V2511::fixOwnerArrow);
        addFix(DataType.ENTITY, "minecraft:spectral_arrow", V2511::fixOwnerArrow);
        addFix(DataType.ENTITY, "minecraft:trident", V2511::fixOwnerArrow);
    }

    private static Value fixOwnerThrowable(Value entity) {
        var owner = entity.remove("owner");
        var mostSignificantBits = owner.get("M").as(Number.class, 0L).longValue();
        var leastSignificantBits = owner.get("L").as(Number.class, 0L).longValue();
        return setOwnerUuid(entity, mostSignificantBits, leastSignificantBits);
    }

    private static Value fixOwnerLlamaSpit(Value entity) {
        var owner = entity.remove("Owner");
        var mostSignificantBits = owner.get("OwnerUUIDMost").as(Number.class, 0L).longValue();
        var leastSignificantBits = owner.get("OwnerUUIDLeast").as(Number.class, 0L).longValue();
        return setOwnerUuid(entity, mostSignificantBits, leastSignificantBits);
    }

    private static Value fixOwnerArrow(Value entity) {
        return UUIDFixes.replaceUuidFromLeastMost(entity, "OwnerUUID", "OwnerUUID");
    }

    private static Value setOwnerUuid(Value entity, long mostSignificantBits, long leastSignificantBits) {
        if (mostSignificantBits == 0L && leastSignificantBits == 0L) return null;
        entity.put("OwnerUUID", UUIDFixes.createUuidArray(mostSignificantBits, leastSignificantBits));
        return null;
    }
}
