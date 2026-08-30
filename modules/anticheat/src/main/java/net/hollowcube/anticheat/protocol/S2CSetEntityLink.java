package net.hollowcube.anticheat.protocol;

/// `play set_entity_link`, keyed per entity. Both ids are plain big-endian ints, not varints.
public sealed interface S2CSetEntityLink extends EntityKeyed permits S2CSetEntityLink.V776 {

    int destEntityId();

    record V776(int entityId, int destEntityId) implements S2CSetEntityLink {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.i32(), reader.i32());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(entityId).i32(destEntityId);
        }
    }
}
