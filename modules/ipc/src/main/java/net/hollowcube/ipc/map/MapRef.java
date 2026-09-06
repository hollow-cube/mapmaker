package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// The two spellings a map is asked for by: its uuid, or the `000-000-000` published id, which
/// players type and which is also accepted without the dashes.
public sealed interface MapRef permits MapRef.Uuid, MapRef.Published {

    record Uuid(UUID id) implements MapRef {}

    record Published(long id) implements MapRef {}

    /// Null for anything that is neither.
    static @Nullable MapRef parse(String text) {
        var digits = text.replace("-", "");
        if (digits.matches("[0-9]{1,9}")) return new Published(Long.parseLong(digits));
        try {
            return new Uuid(UUID.fromString(text));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
