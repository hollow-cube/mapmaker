package net.hollowcube.anticheat.protocol;

/// `play remove_mob_effect`, which deletes the (entity, effect) state cache key.
public sealed interface S2CRemoveMobEffect extends EntityKeyed permits S2CRemoveMobEffect.V776 {

    int effectId();

    record V776(int entityId, int effectId) implements S2CRemoveMobEffect {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).varInt(effectId);
        }
    }
}
