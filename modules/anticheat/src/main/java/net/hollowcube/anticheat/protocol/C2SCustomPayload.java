package net.hollowcube.anticheat.protocol;

/// `common custom_payload`, serverbound. The client's `minecraft:brand` comes in on this one.
public sealed interface C2SCustomPayload extends CustomPayload permits C2SCustomPayload.V776 {

    record V776(String channel, byte[] payload) implements C2SCustomPayload {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.utf(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.utf(channel).bytes(payload);
        }
    }
}
