package net.hollowcube.anticheat.protocol;

/// `play bundle_delimiter` (id 0, registered by `withBundlePacket`). Kept as a frame so a reader
/// can see which packets the client applied in one tick.
public sealed interface S2CBundleDelimiter extends Packet permits S2CBundleDelimiter.V776 {

    record V776() implements S2CBundleDelimiter {

        public static final V776 INSTANCE = new V776();

        public static V776 decode(ByteReader reader) {
            return INSTANCE;
        }

        @Override
        public void encode(ByteWriter writer) {
        }
    }
}
