package net.hollowcube.anticheat.world;

import net.hollowcube.anticheat.protocol.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMapTest {

    private static final int STONE = 1;
    private static final int DIRT = 10;

    @Test
    void testBlockUpdateWritesThroughToTheChunk() {
        var map = loaded(0, 0);

        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(3, 70, 5), STONE));

        assertEquals(STONE, map.blockState(3, 70, 5));
        assertEquals(0, map.blockState(4, 70, 5));
        assertEquals(-1, map.blockState(3, 400, 5), "above build height");
        assertEquals(-1, map.blockState(300, 70, 5), "unloaded chunk");
    }

    @Test
    void testSectionBlocksUpdateWritesEveryEntry() {
        var map = loaded(0, 0);

        long section = Positions.sectionPos(0, 4, 0);
        map.apply(new S2CSectionBlocksUpdate.V776(section, new long[]{
            (long) STONE << 12 | 1 << 8 | 2 << 4 | 3,
            (long) DIRT << 12 | 15 << 8 | 15 << 4 | 15,
        }));

        assertEquals(STONE, map.blockState(1, 64 + 3, 2));
        assertEquals(DIRT, map.blockState(15, 64 + 15, 15));
    }

    @Test
    void testSnapshotIsUnchangedByLaterWrites() {
        var map = loaded(0, 0);
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(0, 70, 0), STONE));

        var view = map.snapshot();
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(0, 70, 0), DIRT));
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(1, 70, 0), DIRT));

        assertEquals(STONE, view.blockState(0, 70, 0));
        assertEquals(0, view.blockState(1, 70, 0));
        assertEquals(DIRT, map.blockState(0, 70, 0));
        assertEquals(DIRT, map.blockState(1, 70, 0));
    }

    @Test
    void testSharedSectionIsClonedExactlyOnce() {
        var map = loaded(0, 0);
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(0, 70, 0), STONE));

        var view = map.snapshot();
        var snapshotted = view.chunk(0, 0);
        assertNotNull(snapshotted);
        long[] shared = snapshotted.sectionData(8);

        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(1, 70, 0), DIRT));
        long[] afterFirstWrite = map.chunk(0, 0).sectionData(8);
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(2, 70, 0), DIRT));
        long[] afterSecondWrite = map.chunk(0, 0).sectionData(8);

        assertNotSame(shared, afterFirstWrite, "the first write clones the shared section");
        assertSame(afterFirstWrite, afterSecondWrite, "and the second writes in place");
        assertNotSame(snapshotted, map.chunk(0, 0), "the chunk itself is copied once too");
        assertSame(shared, snapshotted.sectionData(8), "the snapshot keeps the array it was given");
    }

    @Test
    void testSnapshotIsUnchangedByChunkLoadsAndDrops() {
        var map = loaded(0, 0);
        var view = map.snapshot();

        map.apply(TestPackets.chunk(1, 0, 24));
        map.apply(new S2CForgetLevelChunk.V776(Positions.chunkPos(0, 0)));

        assertEquals(1, view.chunkCount());
        assertNotNull(view.chunk(0, 0));
        assertNull(view.chunk(1, 0));
        assertEquals(1, map.chunkCount());
        assertNull(map.chunk(0, 0));
    }

    @Test
    void testForgetDropsOnlyThatChunk() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(TestPackets.chunk(0, 0, 24));
        map.apply(TestPackets.chunk(1, 0, 24));

        map.apply(new S2CForgetLevelChunk.V776(Positions.chunkPos(1, 0)));

        assertEquals(1, map.chunkCount());
        assertNotNull(map.chunk(0, 0));
    }

    @Test
    void testChunksOutsideTheRingAreNeverLoaded() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2)); // storage radius max(2, 2) + 3 = 5

        assertEquals(5, map.storageRadius());
        map.apply(TestPackets.chunk(5, 0, 24));
        map.apply(TestPackets.chunk(6, 0, 24));

        assertEquals(1, map.chunkCount());
        assertNotNull(map.chunk(5, 0));
    }

    @Test
    void testCacheCentreMoveDropsWhatFallsOutOfTheRing() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        for (int x = -5; x <= 5; x++) map.apply(TestPackets.chunk(x, 0, 24));
        assertEquals(11, map.chunkCount());

        map.apply(new S2CSetChunkCacheCenter.V776(3, 0));

        // ClientChunkCache#inRange keeps |x - centre| <= 5, so -2..5 of what was loaded survives.
        assertEquals(8, map.chunkCount());
        assertNull(map.chunk(-3, 0));
        assertNotNull(map.chunk(-2, 0));
        assertNotNull(map.chunk(5, 0));
    }

    @Test
    void testCacheRadiusChangeDropsLikeUpdateViewRadius() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 10)); // storage radius 13
        for (int x = -13; x <= 13; x++) map.apply(TestPackets.chunk(x, 0, 24));
        assertEquals(27, map.chunkCount());

        map.apply(new S2CSetChunkCacheRadius.V776(11)); // 14, wider, so nothing goes
        assertEquals(27, map.chunkCount());

        map.apply(new S2CSetChunkCacheRadius.V776(2)); // 5
        assertEquals(11, map.chunkCount());
        assertNull(map.chunk(6, 0));
        assertNotNull(map.chunk(5, 0));
    }

    @Test
    void testRadiusBelowTheMinimumStillStoresFiveChunks() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 0));

        // calculateStorageRange is max(2, viewRange) + 3, so a zero view distance is still 5.
        assertEquals(5, map.storageRadius());
    }

    @Test
    void testRespawnClearsOnlyWhenTheDimensionChanges() {
        var map = new ChunkMap();
        map.apply(TestPackets.dimensionTypes(DimensionInfo.OVERWORLD, DimensionInfo.THE_NETHER));
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(TestPackets.chunk(0, 0, 24));

        map.apply(TestPackets.respawn(TestPackets.OVERWORLD, 0));
        assertEquals(1, map.chunkCount(), "respawning in place keeps the level");

        map.apply(TestPackets.respawn(TestPackets.THE_NETHER, 1));
        assertEquals(0, map.chunkCount());
        assertEquals(DimensionInfo.THE_NETHER, map.dimension());
    }

    @Test
    void testLoginAndStartConfigurationClearEverything() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(new S2CSetChunkCacheCenter.V776(4, 4));
        map.apply(TestPackets.chunk(4, 4, 24));
        assertEquals(1, map.chunkCount());

        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        assertEquals(0, map.chunkCount());
        assertEquals(0, map.viewCenterX());
        assertEquals(0, map.viewCenterZ());

        map.apply(TestPackets.chunk(0, 0, 24));
        map.apply(new S2CStartConfiguration.V776());
        assertEquals(0, map.chunkCount());
        assertEquals(DimensionInfo.OVERWORLD, map.dimension());
    }

    @Test
    void testDimensionHeightComesFromTheRegistryEntry() {
        var tall = new DimensionInfo("mapmaker:tall", -128, 512);
        var map = new ChunkMap();
        map.apply(TestPackets.dimensionTypes(DimensionInfo.OVERWORLD, tall));
        map.apply(TestPackets.login("mapmaker:tall", 1, 2));

        assertEquals(tall, map.dimension());
        map.apply(TestPackets.chunk(0, 0, 32));
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(0, -120, 0), STONE));

        assertEquals(STONE, map.blockState(0, -120, 0));
        assertEquals(-1, map.blockState(0, -130, 0));
    }

    @Test
    void testUntouchedChunkKeepsTheSectionsItArrivedWith() {
        var packet = new S2CLevelChunkWithLight.V776(2, -3, TestPackets.heightmaps(),
            sections(TestPackets.palettedSection(0, 9, 42)), ByteSlice.of(new byte[]{1, 2, 3}));

        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(S2CLevelChunkWithLight.V776.decode(new ByteReader(packet.toByteArray())));

        assertArrayEquals(encode(packet.sections()), encode(map.chunk(2, -3).sections()));
    }

    @Test
    void testWrittenChunkKeepsTheWriteInItsSections() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(new S2CLevelChunkWithLight.V776(0, 0, TestPackets.heightmaps(),
            sections(TestPackets.palettedSection(0, 9, 42)), ByteSlice.of(new byte[]{7, 7})));
        map.apply(new S2CBlockUpdate.V776(Positions.blockPos(0, -64, 0), 42));

        assertEquals(42, map.chunk(0, 0).sections().getFirst().get(0, 0, 0));
    }

    @Test
    void testPaletteGrowsAndFallsBackToTheGlobalPalette() {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(new S2CLevelChunkWithLight.V776(0, 0, TestPackets.heightmaps(),
            sections(TestPackets.palettedSection(0)), ByteSlice.of(new byte[0])));

        // Sixteen entries fit the four-bit palette; the seventeenth forces the global palette.
        for (int i = 0; i < 20; i++)
            map.apply(new S2CBlockUpdate.V776(Positions.blockPos(i & 0xF, -64, i >> 4), 100 + i));

        for (int i = 0; i < 20; i++) assertEquals(100 + i, map.blockState(i & 0xF, -64, i >> 4));
        var written = map.chunk(0, 0).sections().getFirst();
        assertEquals(Section.DIRECT_BLOCK_BITS, written.bitsPerEntry());
        assertNull(written.palette());
    }

    private static byte[] encode(List<Section> sections) {
        var writer = new ByteWriter();
        for (Section section : sections) section.encode(writer);
        return writer.toByteArray();
    }

    private static List<Section> sections(Section first) {
        var sections = new ArrayList<Section>();
        sections.add(first);
        for (int i = 1; i < 24; i++) sections.add(TestPackets.airSection());
        return List.copyOf(sections);
    }

    private static ChunkMap loaded(int chunkX, int chunkZ) {
        var map = new ChunkMap();
        map.apply(TestPackets.login(TestPackets.OVERWORLD, 0, 2));
        map.apply(TestPackets.chunk(chunkX, chunkZ, 24));
        return map;
    }
}
