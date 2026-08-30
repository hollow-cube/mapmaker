package net.hollowcube.anticheat.protocol;

/// `play set_entity_motion`. On the local player this is knockback — a direct write to own-player
/// velocity — which is why it is decoded and fenced for self while remote entities go unfenced.
public sealed interface S2CSetEntityMotion extends EntityKeyed permits S2CSetEntityMotion.V776 {

    LpVec3 movement();

    record V776(int entityId, LpVec3 movement) implements S2CSetEntityMotion {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), LpVec3.decode(reader));
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            movement.encode(writer);
        }
    }
}
