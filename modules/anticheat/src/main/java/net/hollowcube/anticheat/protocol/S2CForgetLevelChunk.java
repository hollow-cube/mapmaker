package net.hollowcube.anticheat.protocol;

/// `play forget_level_chunk`. Unloading matters for physics: with no chunk below, the client skips
/// gravity and forces dy = -0.1.
public sealed interface S2CForgetLevelChunk extends Packet permits S2CForgetLevelChunk.V776 {

    int chunkX();

    int chunkZ();

    record V776(long packedChunkPos) implements S2CForgetLevelChunk {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.chunkPos());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.chunkPos(packedChunkPos);
        }

        @Override
        public int chunkX() {
            return Positions.chunkX(packedChunkPos);
        }

        @Override
        public int chunkZ() {
            return Positions.chunkZ(packedChunkPos);
        }
    }
}
