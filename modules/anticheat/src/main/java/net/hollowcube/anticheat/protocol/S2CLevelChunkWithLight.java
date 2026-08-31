package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// `play level_chunk_with_light`: replaces a whole chunk in the client's cache.
///
/// Only the sections are state the model keeps; the heightmaps, the block entities and the light
/// change nothing a body collides with, so nothing above this record sees them.
public sealed interface S2CLevelChunkWithLight extends Packet permits S2CLevelChunkWithLight.V776 {

    int chunkX();

    int chunkZ();

    /// Bottom-to-top, one entry per section of the current dimension's height.
    List<Section> sections();

    /// The heightmaps are kept as the exact bytes of the `Heightmap.Types -> long[]` map, and the
    /// block entities and the whole light payload as one trailing blob, so a decoded packet
    /// re-encodes byte for byte — the round trip is what proves the decoder read the sections
    /// correctly. The capture drops that blob before the frame is stored, so one read back out of
    /// a trace has it empty.
    ///
    /// Both blobs are [ByteSlice] windows into the frame body rather than copies: the decoded
    /// record is dropped as soon as the world model has taken the sections out of it, and the
    /// light payload alone is a large share of every chunk packet.
    record V776(
        int chunkX,
        int chunkZ,
        ByteSlice heightmaps,
        List<Section> sections,
        ByteSlice blockEntitiesAndLight
    ) implements S2CLevelChunkWithLight {

        public static V776 decode(ByteReader reader) {
            int chunkX = reader.i32();
            int chunkZ = reader.i32();

            int heightmapStart = reader.index();
            int heightmapCount = reader.varInt();
            for (int i = 0; i < heightmapCount; i++) {
                reader.varInt(); // heightmap type
                reader.skip(reader.varInt() * 8);
            }
            var heightmaps = reader.sliceSince(heightmapStart);

            var sectionData = reader.slice(reader.varInt());
            var sections = new ArrayList<Section>();
            while (sectionData.remaining() > 0) sections.add(Section.decode(sectionData));

            return new V776(chunkX, chunkZ, heightmaps, List.copyOf(sections), reader.remainingSlice());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(chunkX).i32(chunkZ).bytes(heightmaps);

            var sectionData = new ByteWriter(4096);
            for (var section : sections) section.encode(sectionData);
            writer.byteArray(sectionData.toByteArray());

            writer.bytes(blockEntitiesAndLight);
        }
    }
}
