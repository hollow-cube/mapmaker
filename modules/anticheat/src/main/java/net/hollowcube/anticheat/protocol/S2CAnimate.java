package net.hollowcube.anticheat.protocol;

/// `play animate`. Action [#WAKE_UP] is the one that matters: `stopSleeping` writes the bed block,
/// teleports the entity and changes its pose, so it is the only action that gets a fence.
public sealed interface S2CAnimate extends EntityKeyed permits S2CAnimate.V776 {

    /// `ClientboundAnimatePacket.WAKE_UP` — the rest of the actions are swings and particles.
    int WAKE_UP = 2;

    int action();

    record V776(int entityId, int action) implements S2CAnimate {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.u8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).u8(action);
        }
    }
}
