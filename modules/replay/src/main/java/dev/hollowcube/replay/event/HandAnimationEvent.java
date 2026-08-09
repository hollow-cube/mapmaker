package dev.hollowcube.replay.event;

import net.minestom.server.entity.PlayerHand;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record HandAnimationEvent(int entityId, PlayerHand hand) implements ReplayEvent {
    public static final NetworkBuffer.Type<HandAnimationEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, HandAnimationEvent::entityId,
        PlayerHand.NETWORK_TYPE, HandAnimationEvent::hand,
        HandAnimationEvent::new
    );

}
