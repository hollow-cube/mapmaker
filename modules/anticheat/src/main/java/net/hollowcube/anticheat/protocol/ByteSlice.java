package net.hollowcube.anticheat.protocol;

import java.util.Objects;

/// A read-only window over a `byte[]`, for the parts of a packet a record keeps verbatim without
/// copying them out of the frame body.
///
/// A slice pins the whole backing array, so only a record that dies with the decode may hold one —
/// anything retained past the frame (the world model, the state cache) copies with
/// [#toByteArray()] instead. The trade is deliberate: the body already exists and outlives the
/// decode (the ring keeps it), so a chunk packet's few-hundred-KB light payload was being copied
/// for nothing.
public record ByteSlice(byte[] array, int offset, int length) {

    public ByteSlice {
        Objects.checkFromIndexSize(offset, length, array.length);
    }

    public static ByteSlice of(byte[] array) {
        return new ByteSlice(array, 0, array.length);
    }

    public byte[] toByteArray() {
        var result = new byte[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
    }
}
