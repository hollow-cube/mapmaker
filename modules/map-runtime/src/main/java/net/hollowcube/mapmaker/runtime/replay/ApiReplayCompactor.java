package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.ReplayCompactor;
import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.mapmaker.api.replays.ReplayClient;
import net.hollowcube.mapmaker.api.replays.ReplayCompactionRequest;
import net.hollowcube.mapmaker.api.replays.ReplayRepresentation;
import net.hollowcube.mapmaker.api.replays.ReplayState;
import org.jetbrains.annotations.Blocking;

import java.util.UUID;

/// Compacts a finished recording and publishes it back through the replay storage API.
///
/// Storage marks a replay finished and announces it; this is the Java half that turns that
/// announcement into a playable replay. It is idempotent by design, so whatever triggers it may do
/// so more than once, and a periodic sweep for finished-but-uncompacted replays is a valid trigger
/// on its own.
public final class ApiReplayCompactor {
    private final ReplayClient client;

    public ApiReplayCompactor(ReplayClient client) {
        this.client = client;
    }

    /// Compacts one replay, returning false if there was nothing to do.
    ///
    /// Downloads the finished segmented preamble and every segment it references, compacts them,
    /// and publishes the result conditionally on the source revision. Losing that condition means
    /// someone else got there first, which is a success from this caller's point of view.
    @Blocking
    public boolean compact(String replayId) {
        var resource = client.get(replayId);
        if (resource.state() != ReplayState.FINISHED) return false;
        if (resource.representation() == ReplayRepresentation.COMPACTED) return false;

        var preamble = ReplayPreamble.read(resource.preamble());
        var compacted = ReplayCompactor.compact(
            preamble,
            segmentIndex -> client.getSegment(replayId, segmentIndex).data()
        );

        client.publishCompacted(replayId, new ReplayCompactionRequest(
            resource.etag(),
            UUID.randomUUID(),
            compacted.preambleLength(),
            compacted.data()
        ));
        return true;
    }
}
