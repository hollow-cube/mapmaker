package net.hollowcube.anticheat.protocol;

/// `play move_player_rot`: a rotation with no position.
public sealed interface C2SMovePlayerRot extends MovePlayer permits C2SMovePlayerRot.V776 {

    @Override
    default boolean hasPosition() {
        return false;
    }

    @Override
    default boolean hasRotation() {
        return true;
    }

    record V776(float yRot, float xRot, int flags) implements C2SMovePlayerRot {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.f32(), reader.f32(), reader.u8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.f32(yRot).f32(xRot).u8(flags);
        }
    }
}
