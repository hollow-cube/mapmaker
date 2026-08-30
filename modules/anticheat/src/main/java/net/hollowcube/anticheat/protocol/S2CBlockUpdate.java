package net.hollowcube.anticheat.protocol;

/// `play block_update`, a single block state write.
public sealed interface S2CBlockUpdate extends Packet permits S2CBlockUpdate.V776 {

    /// `BlockPos#asLong`, unpacked with [Positions].
    long packedPos();

    int blockStateId();

    record V776(long packedPos, int blockStateId) implements S2CBlockUpdate {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.blockPos(), reader.varInt());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.blockPos(packedPos).varInt(blockStateId);
        }
    }
}
