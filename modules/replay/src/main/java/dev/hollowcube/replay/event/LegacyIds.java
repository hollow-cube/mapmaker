package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ReplayHeader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/// Format 4's protocol IDs, frozen from the 26.2 registries (`net.minestom:data:26.2-rv3`). Goes
/// with the format 4 reader once every replay has been rewritten.
final class LegacyIds {

    private LegacyIds() {
    }

    static String blockState(int id) {
        return lookup(BlockStates.NAMES, id, "block state");
    }

    static String entityType(int id) {
        return lookup(EntityTypes.NAMES, id, "entity type");
    }

    private static String lookup(String[] names, int id, String kind) {
        if (id < 0 || id >= names.length)
            throw new IllegalStateException("unknown " + kind + " ID " + id + " at data version " + ReplayHeader.LEGACY_IDS_DATA_VERSION);
        return names[id];
    }

    private static String[] load(String resource) {
        var stream = Objects.requireNonNull(LegacyIds.class.getResourceAsStream(resource), resource);
        try (var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8))) {
            return reader.lines().toArray(String[]::new);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + resource, e);
        }
    }

    private static final class BlockStates {
        static final String[] NAMES = load("legacy-ids-4903-block-states.txt.gz");
    }

    private static final class EntityTypes {
        static final String[] NAMES = load("legacy-ids-4903-entity-types.txt.gz");
    }
}
