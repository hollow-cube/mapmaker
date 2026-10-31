package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static net.hollowcube.terraform.nbt.NbtTypes.*;
import static org.junit.jupiter.api.Assertions.*;

/// Edge cases of [NbtStreamWriter]: odd but legal documents, misuse, and failures part-way through a tag.
class NbtStreamWriterEdgeCaseTest {

    @Test
    void zeroLengthWriteIntoAnEmptyByteArrayIsANoop() throws IOException {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        out.beginCompound("");
        out.beginByteArray("empty", 0);
        assertDoesNotThrow(() -> out.writeBytes(new byte[0], 0, 0));
    }

    @Test
    void nestedListsThroughWriteElementMatchAdventure() throws IOException {
        var inner1 = ListBinaryTag.builder()
            .add(IntBinaryTag.intBinaryTag(1))
            .add(IntBinaryTag.intBinaryTag(2))
            .build();
        var inner2 = ListBinaryTag.listBinaryTag(
            BinaryTagTypes.LIST,
            List.of(ListBinaryTag.empty())
        );
        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("");
            out.beginList("ll", LIST, 2);
            out.writeTag(inner1);
            out.writeTag(inner2);
            out.beginList("lc", COMPOUND, 1);
            out.beginCompound();
            out.beginList("deep", COMPOUND, 1);
            out.beginCompound();
            out.writeByte("b", 1);
            out.endCompound();
            out.endCompound();
            out.endCompound();
        }
        var expected = CompoundBinaryTag.builder()
            .put("ll", ListBinaryTag.listBinaryTag(BinaryTagTypes.LIST, List.of(inner1, inner2)))
            .put(
                "lc",
                ListBinaryTag.builder()
                    .add(
                        CompoundBinaryTag.builder()
                            .put(
                                "deep",
                                ListBinaryTag.builder()
                                    .add(CompoundBinaryTag.builder().putByte("b", (byte) 1).build())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();
        assertEquals(Map.entry("", expected), read(bytes.toByteArray()));
    }

    @Test
    void loneSurrogatesNulAndMaxLengthStringsRoundTripThroughAdventure() throws IOException {
        String weird = "\uD800 lone high, \uDC00 lone low, \u0000 nul, ￿, 😀";
        String maxAscii = "a".repeat(65_535);
        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound(weird);
            out.writeString("w", weird);
            out.writeString("max", maxAscii);
            out.writeString("", "");
            out.endCompound();
        }
        var expected = CompoundBinaryTag.builder()
            .putString("w", weird)
            .putString("max", maxAscii)
            .putString("", "")
            .build();
        assertEquals(Map.entry(weird, expected), read(bytes.toByteArray()));
    }

    @Test
    void badOffsetInWriteBytesDoesNotCorruptAccounting() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(bytes)) {
            out.beginCompound("");
            out.beginByteArray("d", 4);
            assertThrows(IndexOutOfBoundsException.class, () -> out.writeBytes(new byte[2], 1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> out.writeBytes(new byte[2], 0, -1));
            out.writeBytes(new byte[] {1, 2, 3, 4}, 0, 4);
            out.endCompound();
        }
        assertEquals(
            Map.entry(
                "",
                CompoundBinaryTag.builder().putByteArray("d", new byte[] {1, 2, 3, 4}).build()
            ),
            read(bytes.toByteArray())
        );
    }

    @Test
    void listFrameOnlyClosesAfterItsLastElementIsFullyWritten() throws IOException {
        var out = new NbtStreamWriter(new ByteArrayOutputStream());
        out.beginCompound("");
        out.beginList("l", COMPOUND, 1);
        out.beginCompound();
        // Still inside the last element: the list must not have popped yet.
        assertThrows(IllegalStateException.class, out::beginCompound);
        out.writeInt("i", 1);
        out.endCompound();
        assertThrows(IllegalStateException.class, out::beginCompound);
        out.endCompound();
        assertDoesNotThrow(out::close);
    }

    @Test
    void closeOnFailureIsSuppressedUnderThePrimaryException() {
        var primary = assertThrows(IllegalStateException.class, () -> {
            try (var out = new NbtStreamWriter(new ByteArrayOutputStream())) {
                out.beginCompound("");
                out.beginList("l", INT, 1);
                out.endCompound(); // list still open
            }
        });
        assertEquals("no compound is open", primary.getMessage());
        assertEquals(1, primary.getSuppressed().length);
    }

    private static Map.Entry<String, CompoundBinaryTag> read(byte[] bytes) throws IOException {
        return BinaryTagIO.unlimitedReader().readNamed(new ByteArrayInputStream(bytes));
    }
}
