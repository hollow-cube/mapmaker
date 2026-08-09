package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record SpawnEntityEvent(int entityId, EntityType entityType, Pos position) implements ReplayEvent {
    public static final NetworkBuffer.Type<SpawnEntityEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, SpawnEntityEvent::entityId,
        EntityType.NETWORK_TYPE, SpawnEntityEvent::entityType,
        NetworkBuffer.POS, SpawnEntityEvent::position,
        SpawnEntityEvent::new
    );
}
