package net.hollowcube.mapmaker.runtime.replay;

import dev.hollowcube.replay.data.ReplayPreamble;
import dev.hollowcube.replay.io.SegmentedReplay;
import dev.hollowcube.replay.io.SegmentedReplayStorage;
import dev.hollowcube.replay.io.SegmentedReplayWriter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.replays.ReplayClient;
import net.hollowcube.mapmaker.api.replays.ReplayRepresentation;
import net.hollowcube.mapmaker.api.replays.ReplayResource;
import net.hollowcube.mapmaker.api.replays.ReplayState;
import org.jetbrains.annotations.Nullable;

/// Segmented replay storage backed by the replay storage API.
public final class ApiSegmentedReplayStorage implements SegmentedReplayStorage {
    private final ReplayClient client;

    public ApiSegmentedReplayStorage(ReplayClient client) {
        this.client = client;
    }

    @Override
    public @Nullable SegmentedReplay load(String replayId) {
        final ReplayResource resource;
        try {
            resource = client.get(replayId);
        } catch (ApiClient.NotFoundError _) {
            return null; // Nothing recorded yet; the first commit creates it.
        }

        // A compacted preamble carries absolute file offsets rather than packed segment offsets, so
        // it cannot be read as a segmented recording. Nothing can append to it either, so all the
        // recorder needs to know is that it is finished.
        if (resource.representation() == ReplayRepresentation.COMPACTED)
            return new SegmentedReplay(null, resource.etag(), true);

        var preamble = ReplayPreamble.read(resource.preamble());
        var nextSegmentIndex = resource.nextSegmentIndex();
        if (nextSegmentIndex != null && nextSegmentIndex != preamble.nextSegmentIndex()) {
            throw new IllegalStateException(
                "replay " + replayId + " expects segment " + nextSegmentIndex
                    + " but its preamble ends at " + preamble.nextSegmentIndex()
            );
        }

        return new SegmentedReplay(preamble, resource.etag(), resource.state() == ReplayState.FINISHED);
    }

    @Override
    public SegmentedReplayWriter writer(String replayId, @Nullable SegmentedReplay base) {
        return new ApiSegmentedReplayWriter(client, replayId, base == null ? null : base.token());
    }
}
