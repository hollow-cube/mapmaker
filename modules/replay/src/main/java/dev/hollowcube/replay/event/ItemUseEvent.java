package dev.hollowcube.replay.event;

import net.minestom.server.entity.PlayerHand;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.Nullable;

/// Which hand an entity is holding an item use in, or null once it stops.
///
/// This is what makes eating, drinking, and drawing a bow read as themselves rather than as
/// standing still holding the item.
public record ItemUseEvent(int entityId, @Nullable PlayerHand hand) implements ReplayEvent {
    public static final NetworkBuffer.Type<ItemUseEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, ItemUseEvent::entityId,
        PlayerHand.NETWORK_TYPE.optional(), ItemUseEvent::hand,
        ItemUseEvent::new
    );
}
