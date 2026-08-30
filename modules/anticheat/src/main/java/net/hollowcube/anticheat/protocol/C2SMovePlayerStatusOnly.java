package net.hollowcube.anticheat.protocol;

/// `play move_player_status_only`: the status byte on its own, sent when neither position nor
/// rotation changed.
public sealed interface C2SMovePlayerStatusOnly extends MovePlayer permits C2SMovePlayerStatusOnly.V776 {

    @Override
    default boolean hasPosition() {
        return false;
    }

    @Override
    default boolean hasRotation() {
        return false;
    }

    record V776(int flags) implements C2SMovePlayerStatusOnly {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.u8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.u8(flags);
        }
    }
}
