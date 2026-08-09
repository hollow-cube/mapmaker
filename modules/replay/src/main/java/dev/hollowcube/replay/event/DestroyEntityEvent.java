package dev.hollowcube.replay.event;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record DestroyEntityEvent(int entityId) implements ReplayEvent {
    public static final NetworkBuffer.Type<DestroyEntityEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, DestroyEntityEvent::entityId,
        DestroyEntityEvent::new
    );
}
