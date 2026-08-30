package net.hollowcube.anticheat.protocol;

/// `play player_position`, the server's own-player teleport. The client applies it (unless it is a
/// passenger), then replies `accept_teleportation` and a forced `move_player_pos_rot`.
public sealed interface S2CPlayerPosition extends Packet permits S2CPlayerPosition.V776 {

    int teleportId();

    PositionMoveRotation change();

    /// [Relative] bits.
    int relatives();

    record V776(int teleportId, PositionMoveRotation change, int relatives) implements S2CPlayerPosition {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), PositionMoveRotation.decode(reader), reader.i32());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(teleportId);
            change.encode(writer);
            writer.i32(relatives);
        }
    }
}
