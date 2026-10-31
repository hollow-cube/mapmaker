package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import static net.hollowcube.terraform.nbt.NbtTypes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Edge cases of [NbtStreamReader], checked against adventure-nbt where they are valid input.
class NbtStreamReaderEdgeCaseTest {

    @Test
    void elementTypeIdsAbove127AreUnknownNotNegative() {
        assertThrows(IOException.class, () -> reader(listHeader(200, 0)).readPayload(LIST));
        assertThrows(IOException.class, () -> reader(listHeader(200, 0)).skipPayload(LIST));
    }

    @Test
    void emptyListOfAnUnknownElementTypeIsRejectedLikeAdventure() throws IOException {
        byte[] payload = listHeader(13, 0);
        // Adventure refuses it ...
        byte[] doc = named(LIST, "l", payload);
        assertThrows(
            RuntimeException.class,
            () -> BinaryTagIO.unlimitedReader().readNamed(new ByteArrayInputStream(doc))
        );
        // ... and so should we.
        assertThrows(IOException.class, () -> reader(payload).skipPayload(LIST));
        assertThrows(IOException.class, () -> reader(payload).readPayload(LIST));
    }

    @Test
    void skipAndReadAgreeWithAdventureOnAMixedDocument() throws IOException {
        var tree = CompoundBinaryTag.builder()
            .putString("s", "héllo \u0000 😀 \uD800")
            .putLongArray("la", new long[] {1, 2, 3})
            .putIntArray("ia", new int[] {-1})
            .putByteArray("ba", new byte[] {0, -1})
            .putFloat("f", Float.NaN)
            .putDouble("d", Double.MIN_VALUE)
            .put("nested", CompoundBinaryTag.builder().putShort("sh", (short) -2).build())
            .build();
        var bytes = new ByteArrayOutputStream();
        BinaryTagIO.writer().writeNamed(Map.entry("root", tree), bytes);
        bytes.write(INT);
        new DataOutputStream(bytes).writeUTF("after");
        new DataOutputStream(bytes).writeInt(7);
        byte[] doc = bytes.toByteArray();

        NbtStreamReader r = reader(doc);
        assertEquals(COMPOUND, r.readType());
        assertEquals("root", r.readString());
        assertEquals(tree, r.readPayload(COMPOUND));
        assertEquals(INT, r.readType());
        assertEquals("after", r.readString());
        assertEquals(7, r.readInt());

        r = reader(doc);
        r.readType();
        r.readString();
        r.skipPayload(COMPOUND);
        assertEquals(INT, r.readType());
        assertEquals("after", r.readString());
    }

    private static byte[] named(int type, String name, byte[] payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(COMPOUND);
        out.writeUTF("");
        out.writeByte(type);
        out.writeUTF(name);
        out.write(payload);
        out.writeByte(END);
        return bytes.toByteArray();
    }

    private static byte[] listHeader(int elem, int len) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(elem);
        out.writeInt(len);
        return bytes.toByteArray();
    }

    private static NbtStreamReader reader(byte[] payload) {
        return new NbtStreamReader(new ByteArrayInputStream(payload));
    }
}
