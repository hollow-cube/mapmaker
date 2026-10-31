package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import static net.hollowcube.terraform.nbt.NbtTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class NbtStreamWriterTest {

    @Test
    void scalarsAndArraysMatchAdventure() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("root");
            out.writeByte("b", -3);
            out.writeShort("s", 0xFFFF);
            out.writeInt("i", Integer.MIN_VALUE);
            out.writeLong("l", Long.MAX_VALUE);
            out.writeString("str", "héllo \u0000 😀");
            out.writeIntArray("ia", 1, -2, 3);
            out.writeIntArray("empty");
            out.endCompound();
        }

        var expected = CompoundBinaryTag.builder()
            .putByte("b", (byte) -3)
            .putShort("s", (short) 0xFFFF)
            .putInt("i", Integer.MIN_VALUE)
            .putLong("l", Long.MAX_VALUE)
            .putString("str", "héllo \u0000 😀")
            .putIntArray("ia", new int[] {1, -2, 3})
            .putIntArray("empty", new int[0])
            .build();
        assertEquals(Map.entry("root", expected), read(bytes.toByteArray()));
    }

    @Test
    void nestedCompoundsListsAndTagsMatchAdventure() throws IOException {
        var data = CompoundBinaryTag.builder()
            .putString("id", "minecraft:chest")
            .put(
                "Items",
                ListBinaryTag.builder()
                    .add(CompoundBinaryTag.builder().putByte("Slot", (byte) 1).build())
                    .build()
            )
            .putLongArray("bits", new long[] {1L, -1L})
            .putDouble("d", 1.5)
            .putFloat("f", -0.25f)
            .build();

        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("");
            out.beginCompound("inner");
            out.writeTag("data", data);
            out.endCompound();
            out.beginList("entries", COMPOUND, 2);
            out.beginCompound();
            out.writeInt("a", 1);
            out.endCompound();
            out.beginCompound();
            out.endCompound();
            out.beginList("strings", STRING, 2);
            out.writeTag(StringBinaryTag.stringBinaryTag("x"));
            out.writeTag(StringBinaryTag.stringBinaryTag("y"));
            out.beginList("none", END, 0);
            out.writeInt("after", 7);
            out.endCompound();
        }

        var expected = CompoundBinaryTag.builder()
            .put("inner", CompoundBinaryTag.builder().put("data", data).build())
            .put(
                "entries",
                ListBinaryTag.builder()
                    .add(CompoundBinaryTag.builder().putInt("a", 1).build())
                    .add(CompoundBinaryTag.empty())
                    .build()
            )
            .put(
                "strings",
                ListBinaryTag.builder()
                    .add(StringBinaryTag.stringBinaryTag("x"))
                    .add(StringBinaryTag.stringBinaryTag("y"))
                    .build()
            )
            .put("none", ListBinaryTag.empty())
            .putInt("after", 7)
            .build();
        assertEquals(Map.entry("", expected), read(bytes.toByteArray()));
    }

    @Test
    void byteArrayStreamedInChunksMatchesAdventure() throws IOException {
        var payload = new byte[100_000];
        new Random(1).nextBytes(payload);

        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("");
            out.beginByteArray("data", payload.length);
            for (int offset = 0; offset < payload.length; offset += 4096) {
                out.writeBytes(payload, offset, Math.min(4096, payload.length - offset));
            }
            out.beginByteArray("empty", 0);
            out.writeInt("after", 1);
            out.endCompound();
        }

        var expected = CompoundBinaryTag.builder()
            .putByteArray("data", payload)
            .putByteArray("empty", new byte[0])
            .putInt("after", 1)
            .build();
        assertEquals(Map.entry("", expected), read(bytes.toByteArray()));
    }

    @Test
    void readerReadsWhatTheWriterWrites() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("");
            out.writeInt("i", 42);
            out.beginByteArray("data", 3);
            out.writeBytes(new byte[] {1, 2, 3}, 0, 3);
            out.endCompound();
        }

        var in = new NbtStreamReader(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(COMPOUND, in.readType());
        assertEquals("", in.readString());
        assertEquals(INT, in.readType());
        assertEquals("i", in.readString());
        assertEquals(42, in.readInt());
        assertEquals(BYTE_ARRAY, in.readType());
        assertEquals("data", in.readString());
        assertEquals(3, in.readLength());
        var data = new byte[3];
        in.readFully(data, 0, 3);
        assertArrayEquals(new byte[] {1, 2, 3}, data);
        assertEquals(END, in.readType());
    }

    @Test
    void rootMustBeANamedCompound() {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        assertThrows(IllegalStateException.class, () -> out.writeInt("i", 1));
        assertThrows(IllegalStateException.class, out::beginCompound);
    }

    @Test
    void listElementsMustMatchTypeAndCount() throws IOException {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        out.beginCompound("");
        out.beginList("l", COMPOUND, 1);
        assertThrows(IllegalStateException.class, () -> out.writeInt("named", 1));
        assertThrows(IllegalStateException.class, () -> out.writeTag(IntBinaryTag.intBinaryTag(1)));
        out.beginCompound();
        out.endCompound();
        assertThrows(IllegalStateException.class, out::beginCompound);
        assertThrows(IllegalArgumentException.class, () -> out.beginList("bad", END, 1));
        assertThrows(IllegalArgumentException.class, () -> out.beginList("bad", (byte) 13, 1));
    }

    @Test
    void byteArrayMustBeFilledExactly() throws IOException {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        out.beginCompound("");
        out.beginByteArray("data", 4);
        out.writeBytes(new byte[2], 0, 2);
        assertThrows(IllegalStateException.class, () -> out.writeInt("early", 1));
        assertThrows(IllegalStateException.class, out::endCompound);
        assertThrows(IllegalStateException.class, () -> out.writeBytes(new byte[3], 0, 3));
        out.writeBytes(new byte[2], 0, 2);
        assertThrows(IllegalStateException.class, () -> out.writeBytes(new byte[1], 0, 1));
        out.endCompound();
        assertDoesNotThrow(out::close);
    }

    @Test
    void closingAnIncompleteDocumentThrows() throws IOException {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        out.beginCompound("");
        assertThrows(IllegalStateException.class, out::close);
    }

    @Test
    void endingWithoutAnOpenCompoundThrows() {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        assertThrows(IllegalStateException.class, out::endCompound);
    }

    private static Map.Entry<String, CompoundBinaryTag> read(byte[] bytes) throws IOException {
        return BinaryTagIO.unlimitedReader().readNamed(new ByteArrayInputStream(bytes));
    }
}
