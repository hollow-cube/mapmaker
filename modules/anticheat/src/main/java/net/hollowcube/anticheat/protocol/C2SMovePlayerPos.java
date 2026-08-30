package net.hollowcube.anticheat.protocol;

/// `play move_player_pos`: a position with no rotation.
public sealed interface C2SMovePlayerPos extends MovePlayer permits C2SMovePlayerPos.V776 {

    @Override
    default boolean hasPosition() {
        return true;
    }

    @Override
    default boolean hasRotation() {
        return false;
    }

    record V776(double x, double y, double z, int flags) implements C2SMovePlayerPos {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.f64(), reader.f64(), reader.f64(), reader.u8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.f64(x).f64(y).f64(z).u8(flags);
        }
    }
}
