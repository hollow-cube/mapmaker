package net.hollowcube.anticheat.protocol;

/// `common ping`. The proxy injects these to bracket state changes, using the negative id space so
/// its pongs are distinguishable from the backend's.
public sealed interface S2CPing extends Packet permits S2CPing.V776 {

    int id();

    record V776(int id) implements S2CPing {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.i32());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(id);
        }
    }
}
