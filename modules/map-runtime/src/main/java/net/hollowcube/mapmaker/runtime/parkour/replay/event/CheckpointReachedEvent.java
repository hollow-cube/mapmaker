package net.hollowcube.mapmaker.runtime.parkour.replay.event;

import dev.hollowcube.replay.event.ReplayEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// A checkpoint the run took, and the time on the clock when it did.
///
/// Parkour has no checkpoint numbering, so the checkpoint is named by the same ID its play state
/// history records, which is the marker entity it belongs to.
///
/// @param checkpointId the checkpoint's ID, as it appears in the play state history
/// @param runTime      the run time in milliseconds at the moment it was reached
public record CheckpointReachedEvent(String checkpointId, long runTime) implements ReplayEvent {
    public static final NetworkBuffer.Type<CheckpointReachedEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.STRING, CheckpointReachedEvent::checkpointId,
        NetworkBuffer.VAR_LONG, CheckpointReachedEvent::runTime,
        CheckpointReachedEvent::new
    );
}
