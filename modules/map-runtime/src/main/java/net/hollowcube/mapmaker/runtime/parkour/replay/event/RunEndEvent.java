package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import dev.hollowcube.replay.event.ReplayEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// The run is over, with the time it ended on and what ended it.
///
/// This is the last event any recording contains: the session is finished immediately after, and a
/// finished replay is never appended to again. A player leaving is not an end, because the run is
/// still theirs to come back to.
///
/// @param reason  what ended the run
/// @param runTime the final run time in milliseconds, which for a completion is the same one the
///                score is computed from
public record RunEndEvent(Reason reason, long runTime) implements ReplayEvent {
    public static final NetworkBuffer.Type<RunEndEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        Reason.NETWORK_TYPE, RunEndEvent::reason,
        NetworkBuffer.VAR_LONG, RunEndEvent::runTime,
        RunEndEvent::new
    );

    /// WARNING: These are positional and baked into every replay recorded so far.
    /// New reasons MUST go on the end. Reordering or deleting is never valid.
    public enum Reason {
        FINISH,
        RESET; // eg fail with no checkpoint or reset item. only hard resets, otherwise CheckpointResetEvent

        public static final NetworkBuffer.Type<Reason> NETWORK_TYPE = NetworkBuffer.Enum(Reason.class);
    }
}
