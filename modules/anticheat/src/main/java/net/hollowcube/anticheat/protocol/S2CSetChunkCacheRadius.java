package net.hollowcube.anticheat.protocol;

/// `play set_chunk_cache_radius`: changes the view radius, which drops or keeps chunks.
public sealed interface S2CSetChunkCacheRadius extends Packet permits S2CSetChunkCacheRadius.V776 {

    int radius();

    record V776(int radius) implements S2CSetChunkCacheRadius {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(radius);
        }
    }
}
