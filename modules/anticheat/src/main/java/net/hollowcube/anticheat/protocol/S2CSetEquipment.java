package net.hollowcube.anticheat.protocol;

/// `play set_equipment`. The slot list cannot be split without decoding item stacks, because the
/// continuation bit sits after each one, so the whole tail is kept.
public sealed interface S2CSetEquipment extends EntityKeyed permits S2CSetEquipment.V776 {

    byte[] rest();

    record V776(int entityId, byte[] rest) implements S2CSetEquipment {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).bytes(rest);
        }
    }
}
