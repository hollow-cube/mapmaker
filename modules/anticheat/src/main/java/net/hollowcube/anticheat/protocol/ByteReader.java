package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

/// A big-endian reader over a `byte[]` window, mirroring the primitives `FriendlyByteBuf` exposes
/// in the 26.2 decompile. Netty-free on purpose: the tap copies frames out of the pipeline as
/// arrays and everything above it (trace reader, tests) only ever sees arrays.
///
/// Every read advances the cursor; running off the end throws [ProtocolException] rather than an
/// unchecked index error, so a malformed frame is a recoverable per-packet failure for the caller.
public final class ByteReader {

    /// Vanilla's string cap: `FriendlyByteBuf#readUtf()` allows 32767 characters and, because
    /// `Utf8String` bounds the encoded form by `ByteBufUtil.utf8MaxBytes`, 3 bytes per character.
    public static final int MAX_STRING_LENGTH = 32767;
    private static final int MAX_STRING_BYTES = MAX_STRING_LENGTH * 3;

    private final byte[] array;
    private final int limit;
    private int index;

    public ByteReader(byte[] array) {
        this(array, 0, array.length);
    }

    public ByteReader(byte[] array, int offset, int length) {
        this.array = array;
        this.index = offset;
        this.limit = offset + length;
    }

    public static ByteReader of(ByteBuffer buffer) {
        if (buffer.hasArray())
            return new ByteReader(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        var copy = new byte[buffer.remaining()];
        buffer.duplicate().get(copy);
        return new ByteReader(copy);
    }

    public int remaining() {
        return limit - index;
    }

    public int index() {
        return index;
    }

    public boolean bool() {
        return u8() != 0;
    }

    public byte i8() {
        require(1);
        return array[index++];
    }

    public int u8() {
        return i8() & 0xFF;
    }

    public short i16() {
        require(2);
        int value = (array[index] & 0xFF) << 8 | array[index + 1] & 0xFF;
        index += 2;
        return (short) value;
    }

    public int u16() {
        return i16() & 0xFFFF;
    }

    public int i32() {
        require(4);
        int value = (array[index] & 0xFF) << 24
            | (array[index + 1] & 0xFF) << 16
            | (array[index + 2] & 0xFF) << 8
            | array[index + 3] & 0xFF;
        index += 4;
        return value;
    }

    public long i64() {
        return ((long) i32() & 0xFFFFFFFFL) << 32 | (long) i32() & 0xFFFFFFFFL;
    }

    public float f32() {
        return Float.intBitsToFloat(i32());
    }

    public double f64() {
        return Double.longBitsToDouble(i64());
    }

    public int varInt() {
        int result = 0;
        int bytes = 0;
        byte read;
        do {
            read = i8();
            result |= (read & 0x7F) << bytes++ * 7;
            if (bytes > 5) throw new ProtocolException("varint too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    public long varLong() {
        long result = 0;
        int bytes = 0;
        byte read;
        do {
            read = i8();
            result |= (long) (read & 0x7F) << bytes++ * 7;
            if (bytes > 10) throw new ProtocolException("varlong too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    public String utf() {
        return utf(MAX_STRING_LENGTH);
    }

    public String utf(int maxLength) {
        int length = varInt();
        if (length < 0 || length > Math.min(maxLength * 3, MAX_STRING_BYTES))
            throw new ProtocolException("string length out of range: " + length);
        require(length);
        var result = new String(array, index, length, StandardCharsets.UTF_8);
        index += length;
        if (result.length() > maxLength)
            throw new ProtocolException("string too long: " + result.length());
        return result;
    }

    public UUID uuid() {
        return new UUID(i64(), i64());
    }

    /// The packed `BlockPos#asLong` form, unpacked with [Positions].
    public long blockPos() {
        return i64();
    }

    /// The packed `ChunkPos#pack` form, unpacked with [Positions].
    public long chunkPos() {
        return i64();
    }

    /// The packed `SectionPos#asLong` form, unpacked with [Positions].
    public long sectionPos() {
        return i64();
    }

    /// varint length, then that many raw bytes (`FriendlyByteBuf#readByteArray`).
    public byte[] byteArray() {
        return bytes(varInt());
    }

    public byte[] bytes(int length) {
        if (length < 0) throw new ProtocolException("negative length: " + length);
        require(length);
        var result = new byte[length];
        System.arraycopy(array, index, result, 0, length);
        index += length;
        return result;
    }

    public byte[] remainingBytes() {
        return bytes(remaining());
    }

    /// A copy of the bytes between an earlier [#index()] and the cursor, for the parts of a packet
    /// that are parsed only far enough to find their length and are then kept verbatim.
    public byte[] since(int startIndex) {
        if (startIndex < 0 || startIndex > index) throw new ProtocolException("bad slice start: " + startIndex);
        var result = new byte[index - startIndex];
        System.arraycopy(array, startIndex, result, 0, result.length);
        return result;
    }

    /// [#since] without the copy: a [ByteSlice] over this reader's backing array, for a field that
    /// dies with the decode. See [ByteSlice] for the retention rule.
    public ByteSlice sliceSince(int startIndex) {
        if (startIndex < 0 || startIndex > index) throw new ProtocolException("bad slice start: " + startIndex);
        return new ByteSlice(array, startIndex, index - startIndex);
    }

    /// [#remainingBytes] without the copy, consuming the rest of the window.
    public ByteSlice remainingSlice() {
        var result = new ByteSlice(array, index, remaining());
        index = limit;
        return result;
    }

    public void skip(int length) {
        require(length);
        index += length;
    }

    public int[] varIntArray() {
        int size = varInt();
        if (size < 0 || size > remaining()) throw new ProtocolException("varint array too big: " + size);
        var result = new int[size];
        for (int i = 0; i < result.length; i++) result[i] = varInt();
        return result;
    }

    public long[] fixedLongArray(int size) {
        var result = new long[size];
        for (int i = 0; i < size; i++) result[i] = i64();
        return result;
    }

    public <T> @Nullable T optional(Function<ByteReader, T> reader) {
        return bool() ? reader.apply(this) : null;
    }

    /// A sub-reader over the next `length` bytes, which are consumed from this reader.
    public ByteReader slice(int length) {
        require(length);
        var slice = new ByteReader(array, index, length);
        index += length;
        return slice;
    }

    /// Skips one whole NBT value, for the packets that embed one and are only parsed far enough
    /// to be split at the right byte boundary. See [Nbt].
    public void skipNbt() {
        Nbt.skip(this);
    }

    private void require(int count) {
        if (count < 0 || limit - index < count)
            throw new ProtocolException("read past end of packet: need " + count + ", have " + remaining());
    }
}
