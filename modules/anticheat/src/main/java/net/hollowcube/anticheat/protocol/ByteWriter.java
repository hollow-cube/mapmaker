package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;

/// The write half of [ByteReader]: a growable big-endian `byte[]` builder producing exactly the
/// bytes vanilla's `FriendlyByteBuf` would have produced for the same values.
public final class ByteWriter {

    private byte[] array;
    private int index;

    public ByteWriter() {
        this(64);
    }

    public ByteWriter(int initialCapacity) {
        this.array = new byte[Math.max(initialCapacity, 16)];
    }

    public int length() {
        return index;
    }

    public byte[] toByteArray() {
        var result = new byte[index];
        System.arraycopy(array, 0, result, 0, index);
        return result;
    }

    /// Starts over in the same backing array, for encode loops that would otherwise allocate a
    /// writer per element.
    public ByteWriter reset() {
        index = 0;
        return this;
    }

    /// Writes everything so far straight to `out`, skipping the copy [#toByteArray] makes.
    public void writeTo(DataOutput out) throws IOException {
        out.write(array, 0, index);
    }

    public ByteWriter bool(boolean value) {
        return u8(value ? 1 : 0);
    }

    public ByteWriter u8(int value) {
        ensure(1);
        array[index++] = (byte) value;
        return this;
    }

    public ByteWriter i16(int value) {
        ensure(2);
        array[index++] = (byte) (value >> 8);
        array[index++] = (byte) value;
        return this;
    }

    public ByteWriter i32(int value) {
        ensure(4);
        array[index++] = (byte) (value >> 24);
        array[index++] = (byte) (value >> 16);
        array[index++] = (byte) (value >> 8);
        array[index++] = (byte) value;
        return this;
    }

    public ByteWriter i64(long value) {
        return i32((int) (value >> 32)).i32((int) value);
    }

    public ByteWriter f32(float value) {
        return i32(Float.floatToRawIntBits(value));
    }

    public ByteWriter f64(double value) {
        return i64(Double.doubleToRawLongBits(value));
    }

    public ByteWriter varInt(int value) {
        while ((value & ~0x7F) != 0) {
            u8(value & 0x7F | 0x80);
            value >>>= 7;
        }
        return u8(value);
    }

    public ByteWriter varLong(long value) {
        while ((value & ~0x7FL) != 0) {
            u8((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        return u8((int) value);
    }

    public ByteWriter utf(String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        return varInt(encoded.length).bytes(encoded);
    }

    public ByteWriter uuid(UUID value) {
        return i64(value.getMostSignificantBits()).i64(value.getLeastSignificantBits());
    }

    public ByteWriter blockPos(long packed) {
        return i64(packed);
    }

    public ByteWriter chunkPos(long packed) {
        return i64(packed);
    }

    public ByteWriter sectionPos(long packed) {
        return i64(packed);
    }

    public ByteWriter byteArray(byte[] value) {
        return varInt(value.length).bytes(value);
    }

    public ByteWriter bytes(byte[] value) {
        return bytes(value, 0, value.length);
    }

    public ByteWriter bytes(byte[] value, int offset, int length) {
        ensure(length);
        System.arraycopy(value, offset, array, index, length);
        index += length;
        return this;
    }

    public ByteWriter bytes(ByteSlice value) {
        return bytes(value.array(), value.offset(), value.length());
    }

    public ByteWriter varIntArray(int[] values) {
        varInt(values.length);
        for (int value : values) varInt(value);
        return this;
    }

    public ByteWriter fixedLongArray(long[] values) {
        for (long value : values) i64(value);
        return this;
    }

    public <T> ByteWriter optional(@Nullable T value, BiConsumer<ByteWriter, T> writer) {
        bool(value != null);
        if (value != null) writer.accept(this, value);
        return this;
    }

    private void ensure(int count) {
        if (array.length - index >= count) return;
        int capacity = Math.max(array.length * 2, index + count);
        var grown = new byte[capacity];
        System.arraycopy(array, 0, grown, 0, index);
        array = grown;
    }
}
