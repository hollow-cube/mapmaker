package net.hollowcube.anticheat.log;

import java.util.List;

/// The world as the trace writer needs it: the chunks that survived trimming, in write order.
///
/// Deliberately the whole of the writer's view of the world model, so `log` never has to see the
/// snapshot, the copy-on-write sections or anything else `world` owns.
@FunctionalInterface
public interface TraceWorld {

    TraceWorld EMPTY = List::of;

    List<WorldChunk> chunks();
}
