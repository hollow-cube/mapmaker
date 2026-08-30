package net.hollowcube.anticheat.protocol;

/// `play update_mob_effect`, keyed by (entity, effect) in the state cache.
public sealed interface S2CUpdateMobEffect extends EntityKeyed permits S2CUpdateMobEffect.V776 {

    int effectId();

    record V776(int entityId, int effectId, int amplifier, int durationTicks, byte flags)
        implements S2CUpdateMobEffect {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.varInt(), reader.varInt(), reader.varInt(), reader.i8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).varInt(effectId).varInt(amplifier).varInt(durationTicks).u8(flags);
        }
    }
}
