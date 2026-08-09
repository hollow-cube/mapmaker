package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// Resets an entity to an exact position, rather than nudging it by a delta.
///
/// Playback accumulates [DeltaMoveEvent] from wherever an entity currently is, so anything the
/// recording did not observe leaves it permanently offset. This re-anchors it: on the first tick of
/// a recording, whenever a session resumes after a gap, and in the snapshot that opens every chunk
/// so that seeking to a chunk does not require replaying every delta before it.
///
/// The velocity is the one the entity ended the tick with. Playback drives position by teleport and
/// so has no use for it, but an analysis over a recording does.
public record AbsoluteMoveEvent(int entityId, Pos position, Vec velocity) implements ReplayEvent {

    public static final NetworkBuffer.Type<AbsoluteMoveEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, AbsoluteMoveEvent::entityId,
        NetworkBuffer.POS, AbsoluteMoveEvent::position,
        NetworkBuffer.LP_VECTOR3, AbsoluteMoveEvent::velocity,
        AbsoluteMoveEvent::new
    );

}
