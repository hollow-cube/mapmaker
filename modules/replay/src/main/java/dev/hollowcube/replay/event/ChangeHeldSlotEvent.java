package dev.hollowcube.replay.event;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ChangeHeldSlotEvent(int entityId, int slot) implements ReplayEvent {
    public static final NetworkBuffer.Type<ChangeHeldSlotEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, ChangeHeldSlotEvent::entityId,
        NetworkBuffer.VAR_INT, ChangeHeldSlotEvent::slot,
        ChangeHeldSlotEvent::new
    );

}
