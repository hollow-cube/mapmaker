package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import dev.hollowcube.replay.event.ReplayEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// The run went back to its last checkpoint, and what sent it there.
///
/// @param reason  what caused the reset
/// @param runTime the run time in milliseconds at the moment of the reset
public record CheckpointResetEvent(Reason reason, long runTime) implements ReplayEvent {
    public static final NetworkBuffer.Type<CheckpointResetEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        Reason.NETWORK_TYPE, CheckpointResetEvent::reason,
        NetworkBuffer.VAR_LONG, CheckpointResetEvent::runTime,
        CheckpointResetEvent::new
    );

    /// Every reset a run can take, one constant per thing that resets a player.
    ///
    /// WARNING: These are positional and baked into every replay recorded so far.
    /// New reasons MUST go on the end. Reordering or deleting is never valid.
    public enum Reason {
        LIVES, // out of lives
        RESET_HEIGHT,
        TIMER,
        MANUAL, // User resetting (eg with checkpoint item)
        FORCED, // Map forced (eg reset marker)
        LIQUID,
        SNEAK,
        TURN,
        SPRINT,
        RELOG;

        public static final NetworkBuffer.Type<Reason> NETWORK_TYPE = NetworkBuffer.Enum(Reason.class);
    }
}
