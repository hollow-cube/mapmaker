package net.hollowcube.anticheat.protocol;

/// `play move_player_pos_rot`: a position and a rotation, the shape a teleport is answered with.
public sealed interface C2SMovePlayerPosRot extends MovePlayer permits C2SMovePlayerPosRot.V776 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return true;
    }

    record V776(double x, double y, double z, float yRot, float xRot, int flags) implements C2SMovePlayerPosRot {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.f64(), reader.f64(), reader.f64(), reader.f32(), reader.f32(), reader.u8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.f64(x).f64(y).f64(z).f32(yRot).f32(xRot).u8(flags);
        }
    }
}
