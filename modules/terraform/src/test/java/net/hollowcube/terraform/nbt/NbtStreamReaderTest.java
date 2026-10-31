package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static net.hollowcube.terraform.nbt.NbtStreamReader.MAX_DEPTH;
import static net.hollowcube.terraform.nbt.NbtStreamReader.MAX_VALUE_BYTES;
import static net.hollowcube.terraform.nbt.NbtTypes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NbtStreamReaderTest {

    @Test
    void deeplyNestedSkipThrowsCleanlyNotStackOverflow() throws IOException {
        NbtStreamReader reader = reader(nestedLists(MAX_DEPTH + 88));
        assertThrows(IOException.class, () -> reader.skipPayload(LIST));
    }

    @Test
    void deeplyNestedReadThrowsCleanlyNotStackOverflow() throws IOException {
        NbtStreamReader reader = reader(nestedLists(MAX_DEPTH + 88));
        assertThrows(IOException.class, () -> reader.readPayload(LIST));
    }

    @Test
    void hostileArrayLengthIsRefusedBeforeAllocation() throws IOException {
        // 2^30 elements declared, none present: refused by the cap, not by end of stream.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new DataOutputStream(bytes).writeInt(1 << 30);
        byte[] payload = bytes.toByteArray();
        assertThrows(IOException.class, () -> reader(payload).readPayload(INT_ARRAY));
        assertThrows(IOException.class, () -> reader(payload).readIntArray());
    }

    @Test
    void shallowListStillReadsAndSkips() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(BYTE);
        out.writeInt(2);
        out.writeByte(0xAA);
        out.writeByte(0xBB);
        byte[] payload = bytes.toByteArray();

        BinaryTag tag = reader(payload).readPayload(LIST);
        assertEquals(2, ((ListBinaryTag) tag).size());

        reader(payload).skipPayload(LIST);
    }

    @Test
    void nonEmptyListOfEndElementsIsRejected() throws IOException {
        // Each END element costs no input, so the skip loop would spin for 2^31 iterations.
        byte[] payload = listHeader(END, Integer.MAX_VALUE);
        assertThrows(IOException.class, () -> reader(payload).skipPayload(LIST));
        assertThrows(IOException.class, () -> reader(payload).readPayload(LIST));
    }

    @Test
    void emptyListOfEndElementsIsAccepted() throws IOException {
        byte[] payload = listHeader(END, 0);
        reader(payload).skipPayload(LIST);
        assertEquals(0, ((ListBinaryTag) reader(payload).readPayload(LIST)).size());
    }

    @Test
    void scalarsAndCompoundsReadInDocumentOrder() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(SHORT);
        out.writeUTF("Width");
        out.writeShort(7);
        out.writeByte(COMPOUND);
        out.writeUTF("Data");
        out.writeByte(STRING);
        out.writeUTF("Id");
        out.writeUTF("minecraft:chest");
        out.writeByte(END);
        out.writeByte(INT);
        out.writeUTF("After");
        out.writeInt(42);
        NbtStreamReader reader = reader(bytes.toByteArray());

        assertEquals(SHORT, reader.readType());
        assertEquals("Width", reader.readString());
        assertEquals(7, reader.readShort());
        assertEquals(COMPOUND, reader.readType());
        assertEquals("Data", reader.readString());
        CompoundBinaryTag data = (CompoundBinaryTag) reader.readPayload(COMPOUND);
        assertEquals("minecraft:chest", data.getString("Id"));
        assertEquals(INT, reader.readType());
        assertEquals("After", reader.readString());
        assertEquals(42, reader.readInt());
    }

    @Test
    void skipPayloadLeavesTheReaderAtTheNextTag() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        // A list of two lists: one holding the int array [1, 2], one holding the string "x".
        out.writeByte(LIST);
        out.writeInt(2);
        out.writeByte(INT_ARRAY);
        out.writeInt(1);
        out.writeInt(2);
        out.writeInt(1);
        out.writeInt(2);
        out.writeByte(STRING);
        out.writeInt(1);
        out.writeUTF("x");
        out.writeByte(INT);
        out.writeUTF("Next");
        NbtStreamReader reader = reader(bytes.toByteArray());

        reader.skipPayload(LIST);
        assertEquals(INT, reader.readType());
        assertEquals("Next", reader.readString());
    }

    @Test
    void negativeArrayLengthIsRejected() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new DataOutputStream(bytes).writeInt(-1);
        byte[] payload = bytes.toByteArray();
        assertThrows(IOException.class, () -> reader(payload).skipPayload(BYTE_ARRAY));
        assertThrows(IOException.class, () -> reader(payload).readIntArray());
    }

    @Test
    void readNumberBoxesEveryNumericType() throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeByte(-1);
        out.writeShort(2);
        out.writeInt(3);
        out.writeLong(4);
        out.writeFloat(5.5f);
        out.writeDouble(6.5);
        NbtStreamReader reader = reader(bytes.toByteArray());
        assertEquals((byte) -1, reader.readNumber(BYTE));
        assertEquals((short) 2, reader.readNumber(SHORT));
        assertEquals(3, reader.readNumber(INT));
        assertEquals(4L, reader.readNumber(LONG));
        assertEquals(5.5f, reader.readNumber(FLOAT));
        assertEquals(6.5, reader.readNumber(DOUBLE));
        assertThrows(IOException.class, () -> reader(new byte[8]).readNumber(STRING));
    }

    @Test
    void unknownTagTypeIsRejected() {
        assertThrows(IOException.class, () -> reader(new byte[0]).skipPayload((byte) 13));
        assertThrows(IOException.class, () -> reader(new byte[0]).readPayload((byte) 13));
    }

    @Test
    void manySmallValuesAddUpToTheLimit() throws IOException {
        // Each empty compound is a few input bytes but counts as a whole tag object.
        int elements = (int) (MAX_VALUE_BYTES / 64);
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeByte(COMPOUND);
        out.writeInt(elements);
        for (int i = 0; i < elements; i++) out.writeByte(END);
        assertThrows(IOException.class, () -> reader(bytes.toByteArray()).readPayload(LIST));
    }

    /// A LIST payload nested `depth` single-element lists deep.
    private static byte[] nestedLists(int depth) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        for (int i = 0; i < depth; i++) {
            out.writeByte(LIST);
            out.writeInt(1);
        }
        return bytes.toByteArray();
    }

    /// A LIST payload header: element type then length, with no elements following.
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
