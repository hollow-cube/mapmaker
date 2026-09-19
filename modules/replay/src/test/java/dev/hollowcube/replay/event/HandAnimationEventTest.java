package dev.hollowcube.replay.event;

import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.component.SwingAnimation;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HandAnimationEventTest {

    @Test
    void roundTripsHandAndAnimation() {
        for (var hand : PlayerHand.values()) {
            for (var type : SwingAnimation.Type.values()) {
                var event = new HandAnimationEvent(7, hand, new SwingAnimation(type, 13));
                var bytes = NetworkBuffer.makeArray(HandAnimationEvent.NETWORK_TYPE, event);
                assertEquals(event, NetworkBuffer.wrap(bytes, 0, bytes.length).read(HandAnimationEvent.NETWORK_TYPE));
            }
        }
    }

    @Test
    void readsSwingsWrittenBeforeTheAnimation() {
        for (var hand : PlayerHand.values()) {
            var bytes = NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, 7);
                buffer.write(PlayerHand.NETWORK_TYPE, hand);
            });
            var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);

            assertEquals(new HandAnimationEvent(7, hand, SwingAnimation.DEFAULT), buffer.read(HandAnimationEvent.NETWORK_TYPE));
            assertEquals(0, buffer.readableBytes());
        }
    }

    @Test
    void refusesAnUnknownAnimationType() {
        var bytes = NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.VAR_INT, 7);
            buffer.write(NetworkBuffer.VAR_INT, 0x2 | 0x3F << 2);
            buffer.write(NetworkBuffer.VAR_INT, 6);
        });

        assertThrows(IllegalArgumentException.class,
            () -> NetworkBuffer.wrap(bytes, 0, bytes.length).read(HandAnimationEvent.NETWORK_TYPE));
    }
}
