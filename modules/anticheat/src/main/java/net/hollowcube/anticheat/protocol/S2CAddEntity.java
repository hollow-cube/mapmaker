package net.hollowcube.anticheat.protocol;

import java.util.UUID;

/// `play add_entity`. The type id decides whether the entity is kept at all: display entities
/// cannot be hit, pushed or collided with, so nothing about them is recorded.
public sealed interface S2CAddEntity extends EntityKeyed permits S2CAddEntity.V776 {

    UUID uuid();

    int entityTypeId();

    double x();

    double y();

    double z();

    /// The byte rotations the wire carries, `value * 360 / 256`.
    byte yRot();

    byte xRot();

    record V776(
        int entityId,
        UUID uuid,
        int entityTypeId,
        double x, double y, double z,
        LpVec3 movement,
        byte xRot, byte yRot, byte yHeadRot,
        int data
    ) implements S2CAddEntity {

        public static V776 decode(ByteReader reader) {
            return new V776(
                reader.varInt(), reader.uuid(), reader.varInt(),
                reader.f64(), reader.f64(), reader.f64(),
                LpVec3.decode(reader),
                reader.i8(), reader.i8(), reader.i8(),
                reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).uuid(uuid).varInt(entityTypeId)
                .f64(x).f64(y).f64(z);
            movement.encode(writer);
            writer.u8(xRot).u8(yRot).u8(yHeadRot).varInt(data);
        }
    }
}
