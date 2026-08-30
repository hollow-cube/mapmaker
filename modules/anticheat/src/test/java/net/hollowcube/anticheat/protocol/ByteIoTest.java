package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ByteIoTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 127, 128, 255, 25565, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void testVarIntRoundTrip(int value) {
        byte[] bytes = new ByteWriter().varInt(value).toByteArray();
        var reader = new ByteReader(bytes);
        assertEquals(value, reader.varInt());
        assertEquals(0, reader.remaining());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 127L, 128L, 2147483647L, -1L, Long.MAX_VALUE, Long.MIN_VALUE})
    void testVarLongRoundTrip(long value) {
        byte[] bytes = new ByteWriter().varLong(value).toByteArray();
        assertEquals(value, new ByteReader(bytes).varLong());
    }

    @Test
    void testVarIntGoldenBytes() {
        assertArrayEquals(new byte[]{(byte) 0xDD, (byte) 0xC7, 0x01}, new ByteWriter().varInt(25565).toByteArray());
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F},
            new ByteWriter().varInt(-1).toByteArray());
    }

    @Test
    void testPrimitives() {
        var uuid = UUID.fromString("b3a2c1d0-0000-4000-8000-0123456789ab");
        byte[] bytes = new ByteWriter()
            .bool(true).u8(0xFE).i16(-2).i32(-3).i64(-4L).f32(1.5F).f64(-2.25).utf("hello ☃").uuid(uuid)
            .toByteArray();
        var reader = new ByteReader(bytes);
        assertTrue(reader.bool());
        assertEquals(0xFE, reader.u8());
        assertEquals((short) -2, reader.i16());
        assertEquals(-3, reader.i32());
        assertEquals(-4L, reader.i64());
        assertEquals(1.5F, reader.f32());
        assertEquals(-2.25, reader.f64());
        assertEquals("hello ☃", reader.utf());
        assertEquals(uuid, reader.uuid());
        assertEquals(0, reader.remaining());
    }

    @Test
    void testBlockPosPacking() {
        // BlockPos#asLong: x 26 bits at 38, z 26 at 12, y 12 at 0.
        long packed = Positions.blockPos(-33554432, -2048, 33554431);
        assertEquals(-33554432, Positions.blockX(packed));
        assertEquals(-2048, Positions.blockY(packed));
        assertEquals(33554431, Positions.blockZ(packed));
        assertEquals(0x0000000000500201L, Positions.blockPos(0, 513, 1280));
    }

    @Test
    void testSectionAndChunkPosPacking() {
        long section = Positions.sectionPos(-7, -1, 9);
        assertEquals(-7, Positions.sectionX(section));
        assertEquals(-1, Positions.sectionY(section));
        assertEquals(9, Positions.sectionZ(section));

        long chunk = Positions.chunkPos(-5, 12);
        assertEquals(-5, Positions.chunkX(chunk));
        assertEquals(12, Positions.chunkZ(chunk));
    }

    @Test
    void testByteArrayAndSlice() {
        byte[] payload = {1, 2, 3, 4};
        byte[] bytes = new ByteWriter().byteArray(payload).varInt(7).toByteArray();
        var reader = new ByteReader(bytes);
        assertArrayEquals(payload, reader.byteArray());
        assertEquals(7, reader.varInt());
    }

    @Test
    void testSkipNbtCompound() {
        // { "a": 1b, "l": [I; 2] } in network form (no root name).
        byte[] nbt = new ByteWriter()
            .u8(10)
            .u8(1).i16(1).bytes("a".getBytes(java.nio.charset.StandardCharsets.UTF_8)).u8(1)
            .u8(11).i16(1).bytes("l".getBytes(java.nio.charset.StandardCharsets.UTF_8)).i32(1).i32(2)
            .u8(0)
            .varInt(42)
            .toByteArray();
        var reader = new ByteReader(nbt);
        reader.skipNbt();
        assertEquals(42, reader.varInt());
    }

    @Test
    void testReadPastEndThrows() {
        var reader = new ByteReader(new byte[]{1});
        assertEquals(1, reader.u8());
        assertThrows(ProtocolException.class, reader::u8);
    }
}
