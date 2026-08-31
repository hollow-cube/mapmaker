package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.common.util.RuntimeGson;

/// How much of the world a trace carries: every chunk within `chunkRadius` of a chunk the player
/// was in, or that an entity came within `entityRange` blocks of the player in.
///
/// A radius of -1 keeps the whole chunk map, for modes that would rather pay the bytes. Trimming is
/// by chunk, never by section, and applies to the world alone — the state cache and the entity
/// table are small and always complete.
@RuntimeGson
public record TrimPolicy(int chunkRadius, int entityRange) {

    /// The plan's default: two chunks around the path, entities within eight blocks.
    public static final TrimPolicy DEFAULT = new TrimPolicy(2, 8);

    /// Everything the client had, for when the trace is worth its full size.
    public static final TrimPolicy EVERYTHING = new TrimPolicy(-1, 8);

    public TrimPolicy {
        if (chunkRadius < -1) throw new IllegalArgumentException("chunk radius must be -1 or more: " + chunkRadius);
        if (entityRange < 0) throw new IllegalArgumentException("entity range must not be negative: " + entityRange);
    }

    public boolean keepsEverything() {
        return chunkRadius < 0;
    }

    public TraceHeader.Trim toHeader() {
        return new TraceHeader.Trim(chunkRadius, entityRange);
    }
}
