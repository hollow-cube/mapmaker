package net.hollowcube.anticheat.protocol;

/// `play teleport_entity`. Also moves the own player when the id is the vehicle the client just
/// removed (`removedPlayerVehicleId`), which is why the reader needs the id and the position.
public sealed interface S2CTeleportEntity extends EntityKeyed permits S2CTeleportEntity.V776 {

    PositionMoveRotation change();

    /// [Relative] bits.
    int relatives();

    boolean onGround();

    record V776(int entityId, PositionMoveRotation change, int relatives, boolean onGround)
        implements S2CTeleportEntity {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), PositionMoveRotation.decode(reader), reader.i32(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            change.encode(writer);
            writer.i32(relatives).bool(onGround);
        }
    }
}
