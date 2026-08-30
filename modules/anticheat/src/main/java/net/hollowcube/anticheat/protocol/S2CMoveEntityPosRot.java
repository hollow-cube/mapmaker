package net.hollowcube.anticheat.protocol;

/// `play move_entity_pos_rot`: a relative move and a rotation.
public sealed interface S2CMoveEntityPosRot extends MoveEntity permits S2CMoveEntityPosRot.V776 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return true;
    }

    record V776(
        int entityId, short deltaX, short deltaY, short deltaZ, byte yRot, byte xRot, boolean onGround
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
}
