package net.hollowcube.anticheat.protocol;

/// `play entity_position_sync`: an absolute position for a tracked entity, and for the player's
/// vehicle a move of the player with it.
public sealed interface S2CEntityPositionSync extends EntityKeyed permits S2CEntityPositionSync.V776 {

    PositionMoveRotation values();

    boolean onGround();

    record V776(int entityId, PositionMoveRotation values, boolean onGround)
        implements S2CEntityPositionSync {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), PositionMoveRotation.decode(reader), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            values.encode(writer);
            writer.bool(onGround);
        }
    }
}
