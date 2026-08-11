package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// Moves an entity by the amount it travelled this tick, with the view it ended the tick facing.
///
/// As in [AbsoluteMoveEvent] the velocity is recorded but not applied on playback.
///
/// The view is the absolute one rather than a change in it, so what [ReplayTypes#LP_POS] rounds off
/// it never accumulates. The travelled distance does accumulate, as playback adds one delta to the
/// next; a recorder anchors the start of every chunk with a full precision [AbsoluteMoveEvent],
/// which clears the drift long before it reaches a hundredth of a block.
public record DeltaMoveEvent(int entityId, Pos delta, Vec velocity) implements ReplayEvent {

    public static final NetworkBuffer.Type<DeltaMoveEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, DeltaMoveEvent::entityId,
        ReplayTypes.LP_POS, DeltaMoveEvent::delta,
        NetworkBuffer.LP_VECTOR3, DeltaMoveEvent::velocity,
        DeltaMoveEvent::new
    );

}
