package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.ByteReader;
import net.hollowcube.anticheat.protocol.ByteWriter;
import net.hollowcube.anticheat.protocol.Section;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// One chunk of the world as the client held it at the snapshot: its block states, section by
/// section. The heightmaps, the block entities and the light the chunk packet carried are not here
/// — none of them can move a body, so a replay never asks for them.
///
/// Sections are bottom-to-top and cover the whole dimension height, so an index here is a section
/// index there. Ones the world model holds as empty are written as [#airSection()], because a
/// missing section is not something the chunk packet can express.
///
/// Layout: `i32 chunkX, i32 chunkZ, varInt sectionCount, section*`.
public record WorldChunk(int chunkX, int chunkZ, List<SectionEntry> sections) {

    public WorldChunk {
        sections = List.copyOf(sections);
    }

    /// How a section is carried. [Inline] is the only thing written today; [ByHash] is the hook for
    /// a content-addressed store, and exists now so adding it later is not a format version bump.
    public sealed interface SectionEntry {

        record Inline(Section section) implements SectionEntry {
        }

        record ByHash(byte[] hash) implements SectionEntry {

            public ByHash {
                if (hash.length != TraceFormat.SECTION_HASH_LENGTH)
                    throw new IllegalArgumentException("section hash must be 32 bytes, got " + hash.length);
            }
        }
    }

    /// An all-air section: no blocks, a single-value block palette of state 0, a single-value biome
    /// palette. Exactly what the client reads for a section the server never filled.
    public static Section airSection() {
        return new Section(0, 0, 0, new int[]{0}, new long[0], new byte[]{0, 0});
    }

    public void encode(DataOutput out) throws IOException {
        out.writeInt(chunkX);
        out.writeInt(chunkZ);
        TraceFormat.writeVarInt(out, sections.size());
        var writer = new ByteWriter(2048);
        for (var entry : sections) {
            switch (entry) {
                case SectionEntry.Inline(Section section) -> {
                    out.writeByte(TraceFormat.SECTION_INLINE);
                    section.encode(writer.reset());
                    TraceFormat.writeVarInt(out, writer.length());
                    writer.writeTo(out);
                }
                case SectionEntry.ByHash(byte[] hash) -> {
                    out.writeByte(TraceFormat.SECTION_BY_HASH);
                    out.write(hash);
                }
            }
        }
    }

    public static WorldChunk decode(DataInput in) throws IOException {
        int chunkX = in.readInt();
        int chunkZ = in.readInt();

        int sectionCount = TraceFormat.readVarInt(in);
        if (sectionCount < 0 || sectionCount > 128)
            throw new TraceFormatException("chunk section count out of range: " + sectionCount);
        var sections = new ArrayList<SectionEntry>(sectionCount);
        for (int i = 0; i < sectionCount; i++) {
            int kind = in.readUnsignedByte();
            switch (kind) {
                case TraceFormat.SECTION_INLINE ->
                    sections.add(new SectionEntry.Inline(Section.decode(new ByteReader(TraceFormat.readBytes(in)))));
                case TraceFormat.SECTION_BY_HASH -> {
                    var hash = new byte[TraceFormat.SECTION_HASH_LENGTH];
                    in.readFully(hash);
                    sections.add(new SectionEntry.ByHash(hash));
                }
                default -> throw new TraceFormatException("unknown section kind: " + kind);
            }
        }

        return new WorldChunk(chunkX, chunkZ, sections);
    }

    @Override
    public String toString() {
        return "WorldChunk[" + chunkX + ", " + chunkZ + ", sections=" + sections.size() + "]";
    }
}
