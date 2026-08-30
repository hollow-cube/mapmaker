package net.hollowcube.ipc.util;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/// The little conversions every service impl repeats: wire strings into value types (a bad one is
/// the caller's 400, not a 500), and `Instant` to and from the epoch millis the wire carries —
/// `java.time` is not a wire scalar.
public final class IpcArgs {

    public static UUID uuid(@Nullable String value, String what) {
        if (value == null) throw new IpcException(400, "missing parameter '" + what + "'");
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IpcException(400, what + " is not a uuid: " + value);
        }
    }

    public static @Nullable Instant instant(@Nullable Long millis) {
        return millis == null ? null : Instant.ofEpochMilli(millis);
    }

    public static @Nullable Long millis(@Nullable Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    private IpcArgs() {}
}
