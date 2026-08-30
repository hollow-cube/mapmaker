package net.hollowcube.anticheat.protocol;

/// `play remove_entities`: drops the per-entity state cache entries for every listed id.
public sealed interface S2CRemoveEntities extends Packet permits S2CRemoveEntities.V776 {

    int[] entityIds();

    record V776(int[] entityIds) implements S2CRemoveEntities {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varIntArray());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varIntArray(entityIds);
        }
    }
}
