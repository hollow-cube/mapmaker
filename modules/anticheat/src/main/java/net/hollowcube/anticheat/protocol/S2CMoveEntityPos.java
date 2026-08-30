package net.hollowcube.anticheat.protocol;

/// `play move_entity_pos`: a relative move with no rotation.
public sealed interface S2CMoveEntityPos extends MoveEntity permits S2CMoveEntityPos.V776 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return false;
    }

    record V776(int entityId, short deltaX, short deltaY, short deltaZ, boolean onGround)
        implements S2CMoveEntityPos {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.i16(), reader.i16(), reader.i16(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).i16(deltaX).i16(deltaY).i16(deltaZ).bool(onGround);
        }
    }
}
