package net.hollowcube.anticheat.protocol;

/// `play player_rotation`: sets the own player's rotation, each axis absolute or relative, and
/// forces a `move_player_rot` reply.
public sealed interface S2CPlayerRotation extends Packet permits S2CPlayerRotation.V776 {

    float yRot();

    boolean relativeYRot();

    float xRot();

    boolean relativeXRot();

    record V776(float yRot, boolean relativeYRot, float xRot, boolean relativeXRot) implements S2CPlayerRotation {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.f32(), reader.bool(), reader.f32(), reader.bool());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.f32(yRot).bool(relativeYRot).f32(xRot).bool(relativeXRot);
        }
    }
}
