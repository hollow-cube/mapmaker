package net.hollowcube.anticheat.protocol;

/// `configuration finish_configuration`: the end of the configuration phase, which the client
/// answers with [C2SFinishConfiguration].
public sealed interface S2CFinishConfiguration extends Packet permits S2CFinishConfiguration.V776 {

    record V776() implements S2CFinishConfiguration {

        public static final V776 INSTANCE = new V776();

        public static V776 decode(ByteReader reader) {
            return INSTANCE;
        }

        @Override
        public void encode(ByteWriter writer) {
        }
    }
}
