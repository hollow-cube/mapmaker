package dev.hollowcube.replay;

import dev.hollowcube.replay.event.ReplayEvent;

import java.util.UUID;

public sealed interface ReplayRecorder permits ReplayRecorderImpl {

    static ReplayRecorder create(UUID worldId, UUID worldVersion) {
        return new ReplayRecorderImpl(worldId, worldVersion);
    }

    /// Advance the replay to the next tick.
    /// This should be called once per tick, after all events have been submitted.
    void advance();

    void submit(ReplayEvent event);

}
