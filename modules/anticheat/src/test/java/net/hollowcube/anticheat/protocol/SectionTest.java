package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SectionTest {

    @Test
    void testSingleValuePaletteRoundTrip() {
        var writer = new ByteWriter();
        writer.i16(0).i16(0);
        writer.u8(0).varInt(1); // block states: single value, stone
        writer.u8(0).varInt(3); // biomes: single value
        byte[] bytes = writer.toByteArray();

        var section = Section.decode(new ByteReader(bytes));
        assertEquals(0, section.nonEmptyBlockCount());
        assertEquals(1, section.get(0, 0, 0));
        assertEquals(1, section.get(15, 15, 15));
        assertArrayEquals(bytes, encode(section));
    }

    @Test
    void testIndirectPaletteRoundTrip() {
        var ids = new int[Section.BLOCK_ENTRY_COUNT];
        for (int i = 0; i < ids.length; i++) ids[i] = i % 3;

        var writer = new ByteWriter();
        writer.i16(4096).i16(0);
        writer.u8(4).varIntArray(new int[]{0, 9, 42});
        writer.fixedLongArray(pack(ids, 4));
        writer.u8(1).varIntArray(new int[]{0, 1}).fixedLongArray(new long[Section.longCount(1, Section.BIOME_ENTRY_COUNT)]);
        byte[] bytes = writer.toByteArray();

        var section = Section.decode(new ByteReader(bytes));
        assertEquals(4096, section.nonEmptyBlockCount());
        assertEquals(0, section.get(0, 0, 0));
        assertEquals(9, section.get(1, 0, 0));
        assertEquals(42, section.get(2, 0, 0));
        assertEquals(9, section.get(0, 1, 0)); // index 256, 256 % 3 == 1
        assertArrayEquals(bytes, encode(section));
    }

    @Test
    void testDirectPaletteRoundTrip() {
        var ids = new int[Section.BLOCK_ENTRY_COUNT];
        for (int i = 0; i < ids.length; i++) ids[i] = i * 7 % 30000;

        var writer = new ByteWriter();
        writer.i16(4096).i16(64);
        writer.u8(Section.DIRECT_BLOCK_BITS);
        writer.fixedLongArray(pack(ids, Section.DIRECT_BLOCK_BITS));
        writer.u8(0).varInt(0);
        byte[] bytes = writer.toByteArray();

        var section = Section.decode(new ByteReader(bytes));
        assertEquals(1024, Section.longCount(Section.DIRECT_BLOCK_BITS, Section.BLOCK_ENTRY_COUNT));
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    assertEquals(ids[(y << 4 | z) << 4 | x], section.get(x, y, z));
        assertArrayEquals(bytes, encode(section));
    }

    @Test
    void testDirectBiomePaletteIsKeptVerbatim() {
        // Biomes above three bits carry no palette; only the declared width sets the array length.
        var writer = new ByteWriter();
        writer.i16(0).i16(0).u8(0).varInt(0);
        writer.u8(7).fixedLongArray(new long[Section.longCount(7, Section.BIOME_ENTRY_COUNT)]);
        byte[] bytes = writer.toByteArray();

        assertArrayEquals(bytes, encode(Section.decode(new ByteReader(bytes))));
    }

    private static byte[] encode(Section section) {
        var writer = new ByteWriter();
        section.encode(writer);
        return writer.toByteArray();
    }

    /// The `SimpleBitStorage` layout: values never straddle a long, low bits first.
    private static long[] pack(int[] ids, int bits) {
        int valuesPerLong = 64 / bits;
        var data = new long[(ids.length + valuesPerLong - 1) / valuesPerLong];
        for (int i = 0; i < ids.length; i++) {
            int cell = i / valuesPerLong;
            data[cell] |= (long) ids[i] << (i - cell * valuesPerLong) * bits;
        }
        return data;
    }
}
