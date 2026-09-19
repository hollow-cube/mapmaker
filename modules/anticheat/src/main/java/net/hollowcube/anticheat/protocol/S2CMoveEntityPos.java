package net.hollowcube.anticheat.protocol;

/// `play move_entity_pos`: a relative move with no rotation.
public sealed interface S2CMoveEntityPos extends MoveEntity permits S2CMoveEntityPos.V776, S2CMoveEntityPos.V777 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return false;
    }

    record V776(int entityId, int deltaX, int deltaY, int deltaZ, boolean onGround)
        implements S2CMoveEntityPos {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.i16(), reader.i16(), reader.i16(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).i16(deltaX).i16(deltaY).i16(deltaZ).bool(onGround);
        }
    }

    /// The trailing on-ground flag moved into a properties varint ahead of the move, whose higher
    /// bits are the step count of a [VecDelta.Stepped] path.
    record V777(int entityId, boolean onGround, VecDelta delta) implements S2CMoveEntityPos, Delta {

        public static V777 decode(ByteReader reader) {
            int entityId = reader.varInt();
            int properties = reader.varInt();
            return new V777(entityId, VecDelta.onGround(properties), VecDelta.decode(reader, properties));
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            delta.encode(writer, onGround);
        }
    }
}
