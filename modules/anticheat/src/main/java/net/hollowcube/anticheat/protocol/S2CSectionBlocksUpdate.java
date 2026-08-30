package net.hollowcube.anticheat.protocol;

/// `play section_blocks_update`: many block writes inside one section.
///
/// The entries are kept as the packed varlongs off the wire (`state << 12 | positionInSection`)
/// so re-encoding cannot change their varlong width.
public sealed interface S2CSectionBlocksUpdate extends Packet permits S2CSectionBlocksUpdate.V776 {

    /// `SectionPos#asLong`, unpacked with [Positions].
    long packedSectionPos();

    long[] entries();

    static int blockStateId(long entry) {
        return (int) (entry >>> 12);
    }

    static int relativeX(long entry) {
        return (int) (entry >> 8 & 0xF);
    }

    static int relativeY(long entry) {
        return (int) (entry & 0xF);
    }

    static int relativeZ(long entry) {
        return (int) (entry >> 4 & 0xF);
    }

    record V776(long packedSectionPos, long[] entries) implements S2CSectionBlocksUpdate {

        public static V776 decode(ByteReader reader) {
            long sectionPos = reader.sectionPos();
            int count = reader.varInt();
            if (count < 0 || count > reader.remaining())
                throw new ProtocolException("bad section update count: " + count);
            var entries = new long[count];
            for (int i = 0; i < count; i++) entries[i] = reader.varLong();
            return new V776(sectionPos, entries);
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.sectionPos(packedSectionPos).varInt(entries.length);
            for (long entry : entries) writer.varLong(entry);
        }
    }
}
