package dev.hollowcube.replay;

import dev.hollowcube.replay.event.ReplayEvent;

public sealed interface ReplayRecorder permits ReplayRecorderImpl {

//    static ReplayRecorder create() {
//
//    }

    /// Advance the replay to the next tick.
    /// This should be called once per tick, after all events have been submitted.
    void advance();

    void submit(ReplayEvent event);

}
