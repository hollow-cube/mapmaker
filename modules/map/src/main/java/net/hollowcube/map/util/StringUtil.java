package net.hollowcube.map.util;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

public final class StringUtil {

    public static @NotNull UUID seededUUID(@NotNull String seed) {
        //todo would be better to either commit to uuids for map ids, or
        var random = new Random(seed.hashCode());
        return new UUID(random.nextLong(), random.nextLong());
    }
}
