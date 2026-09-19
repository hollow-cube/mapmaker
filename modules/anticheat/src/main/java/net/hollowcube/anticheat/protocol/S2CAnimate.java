package net.hollowcube.anticheat.protocol;

/// `play animate`. The wake-up action is the one that matters: `stopSleeping` writes the bed block,
/// teleports the entity and changes its pose, so it is the only action that gets a fence.
public sealed interface S2CAnimate extends EntityKeyed permits S2CAnimate.V776, S2CAnimate.V777 {

    int action();

    boolean wakesUp();

    record V776(int entityId, int action) implements S2CAnimate {

        /// `ClientboundAnimatePacket.WAKE_UP` — the rest of the actions are swings and particles.
        public static final int WAKE_UP = 2;

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.u8());
        }

        @Override
        public boolean wakesUp() {
            return action == WAKE_UP;
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).u8(action);
        }
    }

    /// The swings moved to their own packet, and the ids left behind were renumbered from zero.
    record V777(int entityId, int action) implements S2CAnimate {

        public static final int WAKE_UP = 0;

        public static V777 decode(ByteReader reader) {
            return new V777(reader.varInt(), reader.u8());
        }

        @Override
        public boolean wakesUp() {
            return action == WAKE_UP;
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).u8(action);
        }
    }
}
