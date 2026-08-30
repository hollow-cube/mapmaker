package net.hollowcube.anticheat.protocol;

/// `common custom_payload`, clientbound.
public sealed interface S2CCustomPayload extends CustomPayload permits S2CCustomPayload.V776 {

    record V776(String channel, byte[] payload) implements S2CCustomPayload {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.utf(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.utf(channel).bytes(payload);
        }
    }
}
