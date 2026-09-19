package dev.hollowcube.replay.event;

import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.component.SwingAnimation;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;

/// The hand shares a varint with the animation type, so that a replay recorded before swings had an
/// animation, which wrote only the hand ordinal there, still reads: its swings lack the animation
/// bit and come back as the default whack, which is all a swing could be back then. The duration
/// follows only when that bit is set.
public record HandAnimationEvent(int entityId, PlayerHand hand, SwingAnimation animation) implements ReplayEvent {
    private static final int OFF_HAND = 0x1;
    private static final int HAS_ANIMATION = 0x2;
    private static final int TYPE_SHIFT = 2;

    private static final SwingAnimation.Type[] TYPES = SwingAnimation.Type.values();

    public static final NetworkBuffer.Type<HandAnimationEvent> NETWORK_TYPE = new NetworkBuffer.Type<>() {
        @Override
        public void write(NetworkBuffer buffer, HandAnimationEvent value) {
            var hand = value.hand == PlayerHand.OFF ? OFF_HAND : 0;
            buffer.write(NetworkBuffer.VAR_INT, value.entityId);
            buffer.write(NetworkBuffer.VAR_INT, hand | HAS_ANIMATION | value.animation.type().ordinal() << TYPE_SHIFT);
            buffer.write(NetworkBuffer.VAR_INT, value.animation.duration());
        }

        @Override
        public HandAnimationEvent read(NetworkBuffer buffer) {
            var entityId = buffer.read(NetworkBuffer.VAR_INT);
            var packed = buffer.read(NetworkBuffer.VAR_INT);
            var hand = (packed & OFF_HAND) != 0 ? PlayerHand.OFF : PlayerHand.MAIN;
            if ((packed & HAS_ANIMATION) == 0)
                return new HandAnimationEvent(entityId, hand, SwingAnimation.DEFAULT);

            var type = packed >>> TYPE_SHIFT;
            Check.argCondition(type >= TYPES.length, "invalid swing animation type: {0}", type);
            return new HandAnimationEvent(entityId, hand, new SwingAnimation(TYPES[type], buffer.read(NetworkBuffer.VAR_INT)));
        }
    };

}
