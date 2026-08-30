package net.hollowcube.anticheat.protocol;

/// `play set_passengers`. [#entityId()] is the vehicle: a dropped entity that turns up here as the
/// vehicle of a kept entity has to be promoted back into the capture.
public sealed interface S2CSetPassengers extends EntityKeyed permits S2CSetPassengers.V776 {

    int[] passengerIds();

    record V776(int entityId, int[] passengerIds) implements S2CSetPassengers {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.varIntArray());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId).varIntArray(passengerIds);
        }
    }
}
