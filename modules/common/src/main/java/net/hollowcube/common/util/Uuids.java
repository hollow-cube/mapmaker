package net.hollowcube.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Uuids {
    public static final String ZERO = "00000000-0000-0000-0000-000000000000";

    public static @Nullable UUID parse(@Nullable String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
