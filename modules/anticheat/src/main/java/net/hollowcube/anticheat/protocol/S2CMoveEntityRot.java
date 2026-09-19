package net.hollowcube.anticheat.protocol;

/// `play move_entity_rot`: a rotation with no move.
public sealed interface S2CMoveEntityRot extends MoveEntity permits S2CMoveEntityRot.V776, S2CMoveEntityRot.V777 {

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

    /// The on-ground flag moved ahead of the rotation.
    record V777(int entityId, boolean onGround, byte yRot, byte xRot) implements S2CMoveEntityRot {

        public static V777 decode(ByteReader reader) {
            return new V777(reader.varInt(), reader.bool(), reader.i8(), reader.i8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).bool(onGround).u8(yRot).u8(xRot);
        }
    }
}
