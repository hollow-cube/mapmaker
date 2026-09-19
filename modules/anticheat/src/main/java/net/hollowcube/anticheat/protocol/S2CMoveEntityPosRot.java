package net.hollowcube.anticheat.protocol;

/// `play move_entity_pos_rot`: a relative move and a rotation.
public sealed interface S2CMoveEntityPosRot extends MoveEntity
    permits S2CMoveEntityPosRot.V776, S2CMoveEntityPosRot.V777 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return true;
    }

    record V776(
        int entityId, int deltaX, int deltaY, int deltaZ, byte yRot, byte xRot, boolean onGround
    ) implements S2CMoveEntityPosRot {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.i16(), reader.i16(), reader.i16(),
                reader.i8(), reader.i8(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).i16(deltaX).i16(deltaY).i16(deltaZ)
                .u8(yRot).u8(xRot).bool(onGround);
        }
    }

    /// See [S2CMoveEntityPos.V777]; the rotation still follows the move.
    record V777(int entityId, boolean onGround, VecDelta delta, byte yRot, byte xRot)
        implements S2CMoveEntityPosRot, Delta {

        public static V777 decode(ByteReader reader) {
            int entityId = reader.varInt();
            int properties = reader.varInt();
            var delta = VecDelta.decode(reader, properties);
            return new V777(entityId, VecDelta.onGround(properties), delta, reader.i8(), reader.i8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            delta.encode(writer, onGround);
            writer.u8(yRot).u8(xRot);
        }
    }
}
