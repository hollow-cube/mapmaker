package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.log.WorldChunk;
import net.hollowcube.anticheat.protocol.Positions;
import net.hollowcube.anticheat.world.ChunkMap;
import net.hollowcube.anticheat.world.WorldView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static net.hollowcube.anticheat.capture.TestCapture.SECOND;
import static org.junit.jupiter.api.Assertions.*;

/// The trim region: which chunks a trace keeps, and which of them it can actually produce.
class TrimTest {

    @Test
    void testChunksAreNotedAgainstTheTimeTheyWereSeen() {
        var trim = new Trim();
        trim.add(1 * SECOND, 0, 0);
        trim.add(5 * SECOND, 40, -40);

        assertEquals(Set.of(chunk(0, 0), chunk(2, -3)), trim.since(0));
        assertEquals(Set.of(chunk(2, -3)), trim.since(2 * SECOND));
        assertEquals(Set.of(), trim.since(6 * SECOND));
    }

    @Test
    void testANoteAtANewerTimeKeepsTheChunkAlive() {
        var trim = new Trim();
        trim.add(1 * SECOND, 0, 0);
        trim.add(9 * SECOND, 0, 0);

        assertEquals(1, trim.size());
        assertEquals(Set.of(chunk(0, 0)), trim.since(5 * SECOND));
    }

    @Test
    void testPruningForgetsWhatNoTraceCanStartFrom() {
        var trim = new Trim();
        trim.add(1 * SECOND, 0, 0);
        trim.add(9 * SECOND, 100, 100);
        trim.prune(5 * SECOND);

        assertEquals(1, trim.size());
        assertEquals(Set.of(chunk(6, 6)), trim.since(0));
    }

    @Test
    void testTheRegionIsTheRadiusAroundEachNotedChunk() {
        var world = world(8);
        var region = Trim.region(world, new TrimPolicy(2, 8), Set.of(chunk(0, 0)));

        assertEquals(25, region.size());
        assertTrue(region.contains(chunk(2, -2)));
        assertFalse(region.contains(chunk(3, 0)));
    }

    @Test
    void testTheRegionOnlyKeepsChunksTheClientHad() {
        // Loaded out to four chunks, so the radius around 3,0 runs off the edge of the map.
        var world = world(4);
        var region = Trim.region(world, new TrimPolicy(2, 8), Set.of(chunk(3, 0)));

        assertEquals(20, region.size());
        assertFalse(region.contains(chunk(5, 0)));
    }

    @Test
    void testARadiusOfMinusOneKeepsEverything() {
        var world = world(3);
        var region = Trim.region(world, TrimPolicy.EVERYTHING, Set.of());

        assertEquals(world.chunkCount(), region.size());
        assertEquals(world.chunkCount(), Trim.world(world, TrimPolicy.EVERYTHING, Set.of()).chunks().size());
    }

    @Test
    void testTheTrimmedWorldCarriesTheChunksInOrder() {
        var world = world(8);
        var chunks = Trim.world(world, new TrimPolicy(1, 8), Set.of(chunk(0, 0))).chunks();

        assertEquals(9, chunks.size());
        assertEquals(-1, chunks.getFirst().chunkX());
        assertEquals(-1, chunks.getFirst().chunkZ());
        assertEquals(1, chunks.getLast().chunkX());
        assertEquals(1, chunks.getLast().chunkZ());
        assertEquals(4, chunks.getFirst().sections().size());
    }

    /// A client that loaded every chunk within `radius` of the origin.
    private static WorldView world(int radius) {
        var map = new ChunkMap();
        map.apply(TestCapture.viewDistance(32));
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++) map.apply(TestCapture.chunk(x, z));
        return map.snapshot();
    }

    private static long chunk(int chunkX, int chunkZ) {
        return Positions.chunkPos(chunkX, chunkZ);
    }
}
