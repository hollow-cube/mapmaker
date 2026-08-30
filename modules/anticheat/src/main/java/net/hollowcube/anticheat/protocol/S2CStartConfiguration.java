package net.hollowcube.anticheat.protocol;

/// `play start_configuration`: the client clears its level and switches to the configuration
/// state, which resets the world model and the state cache.
public sealed interface S2CStartConfiguration extends Packet permits S2CStartConfiguration.V776 {

    record V776() implements S2CStartConfiguration {

        public static final V776 INSTANCE = new V776();

        public static V776 decode(ByteReader reader) {
            return INSTANCE;
        }

        @Override
        public void encode(ByteWriter writer) {
        }
    }
}
