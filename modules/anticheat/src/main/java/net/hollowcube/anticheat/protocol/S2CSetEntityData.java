package net.hollowcube.anticheat.protocol;

/// `play set_entity_data`. Metadata values are never decoded in phase 0 (they can hold item
/// stacks); the entity id is all the state cache needs to key on.
public sealed interface S2CSetEntityData extends EntityKeyed permits S2CSetEntityData.V776 {

    byte[] rest();

    record V776(int entityId, byte[] rest) implements S2CSetEntityData {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).bytes(rest);
        }
    }
}
