package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// Moves an entity by the amount it travelled this tick, with the view it ended the tick facing.
///
/// As in [AbsoluteMoveEvent] the velocity is recorded but not applied on playback.
public record DeltaMoveEvent(int entityId, Pos delta, Vec velocity) implements ReplayEvent {

    public static final NetworkBuffer.Type<DeltaMoveEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, DeltaMoveEvent::entityId,
        NetworkBuffer.POS, DeltaMoveEvent::delta,
        NetworkBuffer.LP_VECTOR3, DeltaMoveEvent::velocity,
        DeltaMoveEvent::new
    );

}
