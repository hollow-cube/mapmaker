package net.hollowcube.apiworker.index;

import net.hollowcube.apiworker.index.MapFeatures.Mechanic;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/// Against a real published map — "super fun map", 90 blocks, three checkpoints, two status
/// markers, a spawn that hands out blocks — so that the whole path from polar through the
/// datafixer and the action codecs is what is tested, not a world built to fit the scanner.
class MapIndexerTest {

    static byte[] fixture(String name) throws IOException {
        try (var in = MapIndexerTest.class.getResourceAsStream("/index/" + name)) {
            assertNotNull(in, name);
            return in.readAllBytes();
        }
    }

    @Test
    void geometry() throws IOException {
        var f = MapIndexer.index(fixture("super-fun-map.polar"));

        assertTrue(f.dataVersion() > 0, "a 2026 map carries a data version");
        assertEquals(90, f.blockCount());
        assertEquals(24, f.extentX());
        assertEquals(24, f.extentY());
        assertEquals(64, f.extentZ());
        assertEquals(22, f.occupiedCells());
        assertEquals(16, f.distinctBlocks());
        assertEquals(14.0 / 90, f.dominantBlockFrac(), 1e-9);
        assertEquals(0, f.entityCount(), "nothing but markers in this one");
        assertEquals(0, f.textDisplayCount());
    }

    @Test
    void structureAndMechanics() throws IOException {
        var f = MapIndexer.index(fixture("super-fun-map.polar"));

        assertEquals(3, f.checkpointCount());
        assertEquals(19.235, f.checkpointSpacing(), 1e-3);
        assertEquals(1, f.finishCount());
        assertEquals(2, f.statusCount());

        assertEquals(Set.of(Mechanic.BLOCKS, Mechanic.RESET_HEIGHT), f.mechanics());
        assertEquals(Set.of("scale"), f.attributes());
        assertEquals(Set.of("speed"), f.potionEffects());
        assertEquals(Set.of("reset_in_water"), f.settings());
        assertEquals(6, f.actionCount());
        assertEquals(0, f.decodeFailures());
    }

    /// "little speedrun": eight entities of decoration, five of them text displays, one riding
    /// another, and an older data version that goes through the fixer.
    @Test
    void decoration() throws IOException {
        var f = MapIndexer.index(fixture("decorated.polar"));

        assertEquals(4083, f.dataVersion());
        assertEquals(1793, f.blockCount());
        assertEquals(8, f.entityCount());
        assertEquals(5, f.textDisplayCount());
        assertEquals(4, f.checkpointCount());
        assertEquals(Set.of(Mechanic.TELEPORT, Mechanic.RESET_HEIGHT, Mechanic.TIMER), f.mechanics());
        assertEquals(11, f.actionCount());
        assertEquals(0, f.decodeFailures());
    }

    @Test
    void indexingIsAPureFunctionOfTheBytes() throws IOException {
        var bytes = fixture("super-fun-map.polar");
        assertEquals(MapIndexer.index(bytes), MapIndexer.index(bytes));
    }
}
