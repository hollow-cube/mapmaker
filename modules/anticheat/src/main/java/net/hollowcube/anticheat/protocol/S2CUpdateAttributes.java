package net.hollowcube.anticheat.protocol;

/// `play update_attributes`, keyed per entity.
public sealed interface S2CUpdateAttributes extends EntityKeyed permits S2CUpdateAttributes.V776 {

    byte[] rest();

    record V776(int entityId, byte[] rest) implements S2CUpdateAttributes {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).bytes(rest);
        }
    }
}
