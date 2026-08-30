package net.hollowcube.anticheat.log;

import java.util.List;

/// A whole trace, read into memory.
///
/// [#truncated()] means the file ended inside the body — a crashed writer, a shipped-mid-write
/// file, or a cut download. Everything before the cut is still exactly what was captured, which is
/// why a truncated trace is returned rather than refused.
public record Trace(
    TraceHeader header,
    List<Frame> prelude,
    List<WorldChunk> chunks,
    List<Frame> frames,
    boolean truncated
) {
}
