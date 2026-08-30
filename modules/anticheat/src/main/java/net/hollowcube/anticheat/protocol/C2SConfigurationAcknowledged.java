package net.hollowcube.anticheat.protocol;

/// `play configuration_acknowledged`: the client's reply to [S2CStartConfiguration], and the point at
/// which the tap starts reading configuration-state packet ids.
public sealed interface C2SConfigurationAcknowledged extends Packet permits C2SConfigurationAcknowledged.V776 {

    record V776() implements C2SConfigurationAcknowledged {

        public static final V776 INSTANCE = new V776();

        public static V776 decode(ByteReader reader) {
            return INSTANCE;
        }

        @Override
        public void encode(ByteWriter writer) {
        }
    }
}
