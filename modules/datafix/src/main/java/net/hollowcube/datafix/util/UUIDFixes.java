package net.hollowcube.datafix.util;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class UUIDFixes {


    public static @Nullable Value replaceUuidFromLeastMost(Value value, String prefix, String key) {
        // Funny story here. When doing fixes to Most/Least fields, Mojang accidentally inverts the mostSignificantBits
        // and leastSignificantBits. So we are forced to do the same :sob:

        var mostSignificantBits = value.remove(prefix + "Most").as(Number.class, 0L).longValue();
        var leastSignificantBits = value.remove(prefix + "Least").as(Number.class, 0L).longValue();
        if (mostSignificantBits == 0L && leastSignificantBits == 0L)
            return null;
        value.put(key, createUuidArray(leastSignificantBits, mostSignificantBits));
        return null;
    }

    public static @Nullable Value replaceUuidFromMLTag(Value value, String container, String resultKey) {
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
                // Inverted... nice one mojang...
                value.put(toKey, createUuidArray(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits()));
            } catch (IllegalArgumentException ignored) {
                // Do nothing
            }
        }
        return null;
    }
}
