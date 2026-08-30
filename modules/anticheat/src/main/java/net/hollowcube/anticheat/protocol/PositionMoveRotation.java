package net.hollowcube.anticheat.protocol;

/// `PositionMoveRotation`: absolute-or-relative position, delta movement and rotation, shared by
/// `player_position`, `teleport_entity` and `entity_position_sync`.
public record PositionMoveRotation(
    double x, double y, double z,
    double deltaX, double deltaY, double deltaZ,
    float yRot, float xRot
) {

    public static PositionMoveRotation decode(ByteReader reader) {
        return new PositionMoveRotation(
            reader.f64(), reader.f64(), reader.f64(),
            reader.f64(), reader.f64(), reader.f64(),
            reader.f32(), reader.f32());
    }

    public void encode(ByteWriter writer) {
        writer.f64(x).f64(y).f64(z)
            .f64(deltaX).f64(deltaY).f64(deltaZ)
            .f32(yRot).f32(xRot);
    }
}
