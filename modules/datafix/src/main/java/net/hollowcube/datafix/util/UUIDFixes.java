package net.hollowcube.datafix.util;

import java.util.UUID;

public class UUIDFixes {


    public static Value replaceUuidFromLeastMost(Value value, String prefix, String key) {
        var mostSignificantBits = value.remove(prefix + "Most").as(Number.class, 0L).longValue();
        var leastSignificantBits = value.remove(prefix + "Least").as(Number.class, 0L).longValue();
        value.put(key, createUuidArray(mostSignificantBits, leastSignificantBits));
        return null;
    }

    public static Value replaceUuidFromMLTag(Value value, String container, String resultKey) {
        var mlContainer = value.remove(container);
        var mostSignificantBits = mlContainer.get("M").as(Number.class, 0L).longValue();
        var leastSignificantBits = mlContainer.get("L").as(Number.class, 0L).longValue();
        value.put(resultKey, createUuidArray(mostSignificantBits, leastSignificantBits));
        return null;
    }

    public static int[] createUuidArray(long mostSignificantBits, long leastSignificantBits) {
        return new int[]{
                (int) (leastSignificantBits >> 32),
                (int) leastSignificantBits,
                (int) (mostSignificantBits >> 32),
                (int) mostSignificantBits
        };
    }

    public static Value replaceUuidFromString(Value value, String fromKey, String toKey) {
        if (value.remove(fromKey).value() instanceof String s) {
            try {
                var uuid = UUID.fromString(s);
                value.put(toKey, createUuidArray(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
            } catch (IllegalArgumentException ignored) {
                // Do nothing
            }
        }
        return null;
    }
}
