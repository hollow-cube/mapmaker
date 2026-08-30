package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.S2CLevelChunkWithLight;
import net.hollowcube.anticheat.protocol.Section;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.TestOnly;

/// One chunk of the client's view: the block states, section by section.
///
/// The heightmaps, the block entities and the light the chunk packet also carried are dropped on
/// the way in. A replay only has to put a body where the client put it, and none of the three can
/// move one.
///
/// Copy-on-write works in two steps. [ChunkMap] stamps every chunk with the generation it was
/// created in and bumps that counter on each snapshot, so a chunk from an older generation is one a
/// snapshot still points at and gets [#copy] before a write. The copy shares its sections with the
/// original and owns none of them, so the first write to each section clones it — exactly once,
/// until the next snapshot.
public final class Chunk {

    private final int chunkX;
    private final int chunkZ;
    private final DimensionInfo dimension;
    private final ChunkSection[] sections;
    private final boolean[] owned;
    private final int generation;

    private Chunk(int chunkX, int chunkZ, DimensionInfo dimension, ChunkSection[] sections, int generation) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimension = dimension;
        this.sections = sections;
        this.owned = new boolean[sections.length];
        this.generation = generation;
    }

    static Chunk of(S2CLevelChunkWithLight packet, DimensionInfo dimension, int generation) {
        var decoded = packet.sections();
        var sections = new ChunkSection[decoded.size()];
        for (int i = 0; i < sections.length; i++) sections[i] = ChunkSection.of(decoded.get(i));
        // The sections still belong to the packet record, so none of them are owned yet.
        return new Chunk(packet.chunkX(), packet.chunkZ(), dimension, sections, generation);
    }

    Chunk copy(int generation) {
        return new Chunk(chunkX, chunkZ, dimension, sections.clone(), generation);
    }

    int generation() {
        return generation;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public DimensionInfo dimension() {
        return dimension;
    }

    public int sectionCount() {
        return sections.length;
    }

    /// The sections in wire order, bottom first, each re-encodable byte for byte by
    /// [Section#encode].
    ///
    /// Handing the packed arrays out gives up exclusive ownership of them, so a later write to this
    /// chunk clones the section first and the returned records stay valid.
    public List<Section> sections() {
        var result = new ArrayList<Section>(sections.length);
        for (var section : sections) result.add(section.toSection());
        Arrays.fill(owned, false);
        return List.copyOf(result);
    }

    /// The global block state id at a position given as section-relative x and z and absolute y, or
    /// -1 outside build height.
    public int blockState(int x, int y, int z) {
        int index = dimension.sectionIndex(y);
        if (index < 0 || index >= sections.length) return -1;
        return sections[index].get(x & 0xF, y & 0xF, z & 0xF);
    }

    @TestOnly
    long[] sectionData(int index) {
        return sections[index].data();
    }

    void setBlockState(int x, int y, int z, int stateId) {
        int index = dimension.sectionIndex(y);
        if (index < 0 || index >= sections.length) return;
        if (!owned[index]) {
            sections[index] = sections[index].copy();
            owned[index] = true;
        }
        sections[index].set(x & 0xF, y & 0xF, z & 0xF, stateId);
    }
}
