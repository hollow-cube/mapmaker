package net.hollowcube.terraform.buffer;

import net.minestom.server.instance.palette.Palette;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PaletteHackTest {
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 8, 16, 17, 256, 257, 300})
    void captureAndApplyPreserveValuesCountsAndStorage(int states) {
        Palette original = Palette.blocks();
        for (int i = 0; i < 4096; i++) original.set(i & 15, i >> 8, i >> 4 & 15, i % states);
        Palette captured = PaletteHack.copyWithOffset(original);
        assertEquals(4096, captured.count());
        assertEquals(original.count() == 0 ? 0 : original.bitsPerEntry(), captured.bitsPerEntry());
        Palette restored = Palette.blocks();
        restored.fill(777);
        PaletteHack.applyFromOffset(captured, restored);
        assertEquals(captured.bitsPerEntry(), restored.bitsPerEntry());
        assertSameCells(original, restored);
        assertSameCells(original, roundTrip(restored));
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    assertEquals(original.get(x, y, z) + 1, captured.get(x, y, z));
        original.set(0, 0, 0, 888);
        assertEquals(1, captured.get(0, 0, 0));
        captured.set(1, 0, 0, 999);
        assertEquals(1 % states, restored.get(1, 0, 0));
        restored.set(2, 0, 0, 888);
        restored.set(3, 0, 0, 0);
        assertSameCells(restored, roundTrip(restored));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void narrowEditPalettesWidenForWorldSerialization(int bits) {
        int[] dictionary = new int[1 << bits];
        for (int i = 0; i < dictionary.length; i++) dictionary[i] = i + 1;
        int[] indices = new int[4096];
        for (int i = 0; i < indices.length; i++) indices[i] = i % dictionary.length;
        Palette edits = Palette.empty(16, 1, 8, 16);
        edits.load(dictionary, Palettes.pack(indices, bits));
        Palette world = Palette.blocks();
        PaletteHack.applyFromOffset(edits, world);
        assertEquals(4, world.bitsPerEntry());
        assertEquals(2048, world.indexedValues().length * Long.BYTES);
        assertEquals(4096 - 4096 / dictionary.length, world.count());
        assertSameCells(world, roundTrip(world));
        for (int i = 0; i < indices.length; i++)
            assertEquals(indices[i], world.get(i & 15, i >> 8, i >> 4 & 15));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void unusedLeaveSlotsAreRemovedWithoutDuplicateAir(int leaveIndex) {
        int[] dictionary = {1, 2, 3, 4};
        dictionary[leaveIndex] = 0;
        int[] indices = new int[4096];
        for (int i = 0; i < indices.length; i++) {
            int index = i % 3;
            indices[i] = index >= leaveIndex ? index + 1 : index;
        }
        Palette edits = Palette.empty(16, 1, 8, 16);
        edits.load(dictionary, Palettes.pack(indices, 2));
        assertEquals(4096, edits.count());
        Palette world = Palette.blocks();
        PaletteHack.applyFromOffset(edits, world);
        for (int i = 0; i < indices.length; i++)
            assertEquals(dictionary[indices[i]] - 1, world.get(i & 15, i >> 8, i >> 4 & 15));
        assertEquals(3, PaletteHack.dictionary(world).size());
        assertSameCells(world, roundTrip(world));
        world.set(0, 0, 0, 0);
        world.set(1, 0, 0, 2);
        world.set(2, 0, 0, 999);
        assertSameCells(world, roundTrip(world));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void duplicateLoadedValuesAreMergedAndCounted(int solidIndex) {
        int[] dictionary = {1, 1, 1, 1};
        dictionary[solidIndex] = 2;
        int[] indices = new int[4096];
        for (int i = 0; i < indices.length; i++) indices[i] = i % 4;
        Palette edits = Palette.empty(16, 1, 8, 16);
        edits.load(dictionary, Palettes.pack(indices, 2));
        Palette world = Palette.blocks();
        PaletteHack.applyFromOffset(edits, world);
        assertEquals(1024, world.count());
        assertEquals(2, PaletteHack.dictionary(world).size());
        assertSameCells(world, roundTrip(world));
        for (int i = 0; i < indices.length; i++)
            assertEquals(i % 4 == solidIndex ? 1 : 0, world.get(i & 15, i >> 8, i >> 4 & 15));
    }

    @Test
    void packedAirAndUniformSectionsStayUniform() {
        Palette air = Palette.blocks();
        air.set(0, 0, 0, 1);
        air.set(0, 0, 0, 0);
        assertTrue(air.bitsPerEntry() > 0);
        Palette captured = PaletteHack.copyWithOffset(air);
        assertEquals(0, captured.bitsPerEntry());
        assertEquals(1, captured.singleValue());
        Palette world = Palette.blocks();
        PaletteHack.applyFromOffset(captured, world);
        assertEquals(0, world.bitsPerEntry());
        assertEquals(0, world.count());
        air.fill(42);
        captured = PaletteHack.copyWithOffset(air);
        assertEquals(0, captured.bitsPerEntry());
        assertEquals(43, captured.singleValue());
        PaletteHack.applyFromOffset(captured, world);
        assertEquals(42, world.singleValue());
    }

    @Test
    void directBoundaryValuesAreTranslatedWithoutTruncation() {
        Palette original = Palette.blocks(Palette.BLOCK_PALETTE_DIRECT_BITS);
        original.setAll((x, _, _) -> x == 0 ? 0 : 65534);
        Palette captured = PaletteHack.copyWithOffset(original);
        assertEquals(65535, captured.get(1, 0, 0));
        Palette restored = Palette.blocks();
        PaletteHack.applyFromOffset(captured, restored);
        assertSameCells(original, restored);
        assertSameCells(original, roundTrip(restored));
        original.set(0, 0, 0, 65535);
        assertThrows(IllegalArgumentException.class, () -> PaletteHack.copyWithOffset(original));
    }

    @Test
    void partialApplyFailsWithoutChangingDestination() {
        Palette edits = Palette.empty(16, 1, 8, 16);
        edits.set(0, 0, 0, 1);
        Palette world = Palette.blocks();
        world.fill(42);
        assertThrows(
            IllegalArgumentException.class,
            () -> PaletteHack.applyFromOffset(edits, world)
        );
        assertEquals(42, world.singleValue());
    }

    private static Palette roundTrip(Palette palette) {
        var type = Palette.serializer(16, 4, 8, Palette.BLOCK_PALETTE_DIRECT_BITS);
        byte[] bytes = NetworkBuffer.makeArray(buffer -> buffer.write(type, palette));
        return NetworkBuffer.wrap(bytes, 0, bytes.length).read(type);
    }

    private static void assertSameCells(Palette expected, Palette actual) {
        int nonzero = 0;
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    int value = expected.get(x, y, z);
                    assertEquals(value, actual.get(x, y, z));
                    if (value != 0) nonzero++;
                }
        assertEquals(nonzero, actual.count());
    }
}
