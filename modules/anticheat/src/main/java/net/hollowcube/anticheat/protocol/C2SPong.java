package net.hollowcube.anticheat.protocol;

/// `common pong`, the client's reply to [S2CPing].
public sealed interface C2SPong extends Packet permits C2SPong.V776 {

    int id();

    record V776(int id) implements C2SPong {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.i32());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(id);
        }
    }
}
