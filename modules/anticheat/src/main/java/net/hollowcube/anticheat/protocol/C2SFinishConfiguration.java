package net.hollowcube.anticheat.protocol;

/// `configuration finish_configuration`, the client's acknowledgement of [S2CFinishConfiguration]
/// and the point at which it starts reading play-state packet ids.
public sealed interface C2SFinishConfiguration extends Packet permits C2SFinishConfiguration.V776 {

    record V776() implements C2SFinishConfiguration {

        public static final V776 INSTANCE = new V776();

        public static V776 decode(ByteReader reader) {
            return INSTANCE;
        }

        @Override
        public void encode(ByteWriter writer) {
        }
    }
}
