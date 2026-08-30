package net.hollowcube.anticheat.protocol;

/// `play set_chunk_cache_center`: moves the client's chunk cache, dropping what falls outside it.
public sealed interface S2CSetChunkCacheCenter extends Packet permits S2CSetChunkCacheCenter.V776 {

    int chunkX();

    int chunkZ();

    record V776(int chunkX, int chunkZ) implements S2CSetChunkCacheCenter {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(chunkX).varInt(chunkZ);
        }
    }
}
