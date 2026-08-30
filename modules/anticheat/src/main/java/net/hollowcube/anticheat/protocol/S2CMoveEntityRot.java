package net.hollowcube.anticheat.protocol;

/// `play move_entity_rot`: a rotation with no move.
public sealed interface S2CMoveEntityRot extends MoveEntity permits S2CMoveEntityRot.V776 {

    @Override
    default boolean hasPosition() {
        return false;
    }

    @Override
    default boolean hasRotation() {
        return true;
    }

    record V776(int entityId, byte yRot, byte xRot, boolean onGround) implements S2CMoveEntityRot {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.i8(), reader.i8(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).u8(yRot).u8(xRot).bool(onGround);
        }
    }
}
