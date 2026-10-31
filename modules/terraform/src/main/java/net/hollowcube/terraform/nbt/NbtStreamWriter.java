package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;

import java.io.*;
import java.util.ArrayDeque;

import static net.hollowcube.terraform.nbt.NbtTypes.*;

/// A push-based NBT writer.
public final class NbtStreamWriter implements Closeable {
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final DataOutputStream out;

    public NbtStreamWriter(OutputStream out) {
        // Buffered so per-tag writes don't spam the output.
        this.out = new DataOutputStream(new BufferedOutputStream(out, 1 << 16));
    }

    /// Opens a named compound: the root, or a tag in the current compound.
    public void beginCompound(String name) throws IOException {
        if (!frames.isEmpty()) requireCompound();
        out.writeByte(COMPOUND);
        out.writeUTF(name);
        frames.push(Frame.compound());
    }

    /// Opens a compound element of the current list.
    public void beginCompound() throws IOException {
        element(COMPOUND);
        frames.push(Frame.compound());
    }

    public void endCompound() throws IOException {
        Frame frame = frames.peek();
        if (frame == null || frame.type != COMPOUND) {
            throw new IllegalStateException("no compound is open");
        }
        out.writeByte(END);
        frames.pop();
        elementDone();
    }

    public void writeByte(String name, int value) throws IOException {
        header(BYTE, name);
        out.writeByte(value);
    }

    public void writeShort(String name, int value) throws IOException {
        header(SHORT, name);
        out.writeShort(value);
    }

    public void writeInt(String name, int value) throws IOException {
        header(INT, name);
        out.writeInt(value);
    }

    public void writeLong(String name, long value) throws IOException {
        header(LONG, name);
        out.writeLong(value);
    }

    public void writeString(String name, String value) throws IOException {
        header(STRING, name);
        out.writeUTF(value);
    }

    public void writeIntArray(String name, int... values) throws IOException {
        header(INT_ARRAY, name);
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    public void writeTag(String name, BinaryTag tag) throws IOException {
        header(tag.type().id(), name);
        writePayload(tag);
    }

    public void writeTag(BinaryTag tag) throws IOException {
        element(tag.type().id());
        writePayload(tag);
        elementDone();
    }

    public void beginByteArray(String name, int length) throws IOException {
        if (length < 0) throw new IllegalArgumentException("negative byte array length " + length);
        header(BYTE_ARRAY, name);
        out.writeInt(length);
        if (length > 0) frames.push(Frame.byteArray(length));
    }

    public void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) return;
        Frame frame = frames.peek();
        if (frame == null || frame.type != BYTE_ARRAY) {
            throw new IllegalStateException("no byte array is open");
        }
        if (length > frame.remaining) {
            throw new IllegalStateException(
                "byte array overflow: %d bytes written with %d remaining".formatted(
                    length,
                    frame.remaining
                )
            );
        }
        out.write(bytes, offset, length);
        frame.remaining -= length;
        if (frame.remaining == 0) frames.pop();
    }

    /// Opens a list of `length` elements of `elementType`, which then must be written in full before any
    /// other tag. An empty list takes [NbtTypes#END] as its element type.
    public void beginList(String name, byte elementType, int length) throws IOException {
        if (length < 0) throw new IllegalArgumentException("negative list length " + length);
        if (elementType < END || elementType > LONG_ARRAY) {
            throw new IllegalArgumentException("unknown element type " + elementType);
        }
        if (elementType == END && length > 0) {
            throw new IllegalArgumentException("a list of END elements must be empty");
        }
        header(LIST, name);
        out.writeByte(elementType);
        out.writeInt(length);
        if (length > 0) frames.push(Frame.list(elementType, length));
    }

    @Override
    public void close() throws IOException {
        out.close();
        if (!frames.isEmpty()) throw new IllegalStateException("closed with tags still open");
    }

    private void header(byte type, String name) throws IOException {
        requireCompound();
        out.writeByte(type);
        out.writeUTF(name);
    }

    private void requireCompound() {
        Frame frame = frames.peek();
        if (frame == null) {
            throw new IllegalStateException("a tag must go in a compound");
        }
        if (frame.type != COMPOUND) {
            throw new IllegalStateException(
                frame.type == LIST
                    ? "a named tag cannot go in a list"
                    : "the open byte array needs %d more bytes".formatted(frame.remaining)
            );
        }
    }

    private void element(byte type) {
        Frame frame = frames.peek();
        if (frame == null || frame.type != LIST) throw new IllegalStateException("no list is open");
        if (frame.elementType != type) {
            throw new IllegalStateException(
                "list of type %d cannot hold type %d".formatted(frame.elementType, type)
            );
        }
        frame.remaining--;
    }

    private void elementDone() {
        Frame frame = frames.peek();
        if (frame != null && frame.type == LIST && frame.remaining == 0) frames.pop();
    }

    @SuppressWarnings("unchecked")
    private void writePayload(BinaryTag tag) throws IOException {
        ((BinaryTagType<BinaryTag>) tag.type()).write(tag, out);
    }

    private static final class Frame {
        final byte type;
        final byte elementType;
        long remaining;

        static Frame compound() {
            return new Frame(COMPOUND, END, 0);
        }

        static Frame list(byte elementType, int length) {
            return new Frame(LIST, elementType, length);
        }

        static Frame byteArray(int length) {
            return new Frame(BYTE_ARRAY, END, length);
        }

        private Frame(byte type, byte elementType, long remaining) {
            this.type = type;
            this.elementType = elementType;
            this.remaining = remaining;
        }
    }
}
