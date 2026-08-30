package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Metadata776Test {

    @Test
    void testEntriesSplitOnTheirBoundaries() {
        byte[] flags = new ByteWriter().u8(0).varInt(0).u8(0x02).toByteArray();
        byte[] width = new ByteWriter().u8(8).varInt(3).f32(1.5f).toByteArray();
        var payload = new ByteWriter().bytes(flags).bytes(width).u8(Metadata776.TERMINATOR).toByteArray();

        var entries = Metadata776.entries(payload);
        assertEquals(2, entries.size());
        assertEquals(0, entries.get(0).index());
        assertArrayEquals(flags, entries.get(0).bytes());
        assertEquals(8, entries.get(1).index());
        assertEquals(3, entries.get(1).serializerId());
        assertArrayEquals(width, entries.get(1).bytes());
    }

    /// Item stacks cannot be walked without the component codecs, so the split ends there — with
    /// everything before it still whole, and never a guessed length that would misread the rest.
    @Test
    void testAnUnwalkableSerializerEndsTheSplitAfterWhatCame() {
        var payload = new ByteWriter()
            .u8(0).varInt(0).u8(0x02)
            .u8(5).varInt(7).u8(1) // ITEM_STACK, whatever follows
            .u8(8).varInt(3).f32(1.5f)
            .u8(Metadata776.TERMINATOR)
            .toByteArray();

        var entries = Metadata776.entries(payload);
        assertEquals(1, entries.size());
        assertEquals(0, entries.getFirst().index());
    }

    @Test
    void testACutPayloadYieldsWhatWasWhole() {
        var whole = new ByteWriter().u8(0).varInt(0).u8(0x02).u8(6).varInt(4).toByteArray();
        // The string entry's length varint promises bytes that never come.
        var cut = new ByteWriter().bytes(whole).varInt(20).u8('x').toByteArray();

        var entries = Metadata776.entries(cut);
        assertEquals(1, entries.size());
        assertEquals(0, entries.getFirst().index());
    }

    @Test
    void testOptionalsWithoutAValueAreTheirOwnWholeEntry() {
        var payload = new ByteWriter()
            .u8(2).varInt(6).bool(false)  // optional component, absent
            .u8(3).varInt(11).bool(false) // optional block pos, absent
            .u8(Metadata776.TERMINATOR)
            .toByteArray();

        assertEquals(2, Metadata776.entries(payload).size());
    }
}
