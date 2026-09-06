package net.hollowcube.compat.axiom;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/// Reassembles one API 10 `axiom:tunnel` message per player. Fragments are one flag byte followed by data;
/// the assembled message is a channel identifier, an Int uncompressed size, a flag byte and the raw or Zstd payload.
@NotNullByDefault
final class AxiomTunnel {

    static final int FRAGMENT_FIRST = 1;
    static final int FRAGMENT_LAST = 2;
    static final int PACKET_ZSTD = 1;
    private static final byte[] EMPTY = new byte[0];

    private byte[] buffer = EMPTY;
    private int length = 0;
    private boolean open = false;

    static final class MalformedException extends Exception {
        MalformedException(String message) {
            super(message);
        }

        MalformedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    record Message(String channel, byte[] payload) {
    }

    /// A malformed fragment discards the partial message before throwing.
    byte @Nullable [] accept(byte[] fragment) throws MalformedException {
        if (fragment.length > AxiomAPI.TUNNEL_SPLIT_SIZE)
            fail("fragment of " + fragment.length + " bytes exceeds the split size");
        if (fragment.length < 2)
            fail("empty fragment");
        int flags = fragment[0];
        if ((flags & ~(FRAGMENT_FIRST | FRAGMENT_LAST)) != 0)
            fail("unknown fragment flags " + flags);
        boolean first = (flags & FRAGMENT_FIRST) != 0;
        boolean last = (flags & FRAGMENT_LAST) != 0;

        if (first) {
            reset();
        } else if (!open) {
            fail("continuation fragment without a first fragment");
        }

        int dataLength = fragment.length - 1;
        if (this.length + dataLength > AxiomAPI.MAX_TUNNEL_PACKET_SIZE)
            fail("message exceeds " + AxiomAPI.MAX_TUNNEL_PACKET_SIZE + " bytes");
        ensureCapacity(this.length + dataLength);
        System.arraycopy(fragment, 1, this.buffer, this.length, dataLength);
        this.length += dataLength;
        this.open = true;

        if (!last) return null;
        byte[] assembled = Arrays.copyOf(this.buffer, this.length);
        reset();
        return assembled;
    }

    void reset() {
        this.buffer = EMPTY;
        this.length = 0;
        this.open = false;
    }

    boolean hasPartial() {
        return this.open;
    }

    private void ensureCapacity(int required) {
        if (this.buffer.length >= required) return;
        int grown = Math.max(this.buffer.length * 2, AxiomAPI.TUNNEL_SPLIT_SIZE);
        int capacity = Math.max(required, Math.min(grown, AxiomAPI.MAX_TUNNEL_PACKET_SIZE));
        this.buffer = Arrays.copyOf(this.buffer, capacity);
    }

    private void fail(String message) throws MalformedException {
        reset();
        throw new MalformedException(message);
    }

    static Message decode(byte[] assembled) throws MalformedException {
        var buffer = NetworkBuffer.wrap(assembled, 0, assembled.length);
        String channel;
        try {
            channel = Key.key(buffer.read(NetworkBuffer.STRING)).asString();
        } catch (IndexOutOfBoundsException | IllegalArgumentException | InvalidKeyException e) {
            throw new MalformedException("invalid tunnel channel", e);
        }
        if (buffer.readableBytes() < Integer.BYTES + Byte.BYTES)
            throw new MalformedException("truncated tunnel header for " + channel);

        int size = buffer.read(NetworkBuffer.INT);
        byte flags = buffer.read(NetworkBuffer.BYTE);
        if (size < 0 || size > AxiomAPI.MAX_TUNNEL_PACKET_SIZE)
            throw new MalformedException("declared payload size " + size + " for " + channel + " is out of bounds");
        if ((flags & ~PACKET_ZSTD) != 0)
            throw new MalformedException("unknown tunnel packet flags " + flags);

        int offset = (int) buffer.readIndex();
        int remaining = (int) buffer.readableBytes();
        if ((flags & PACKET_ZSTD) == 0) {
            if (remaining != size)
                throw new MalformedException("raw payload of " + remaining + " bytes does not match declared size " + size);
            return new Message(channel, Arrays.copyOfRange(assembled, offset, offset + size));
        }

        byte[] payload = new byte[size];
        long decompressed;
        try {
            decompressed = Zstd.decompressByteArray(payload, 0, size, assembled, offset, remaining);
        } catch (ZstdException e) {
            throw new MalformedException("zstd failed to decompress " + channel, e);
        }
        if (decompressed != size)
            throw new MalformedException("decompressed " + decompressed + " bytes but " + size + " were declared");
        return new Message(channel, payload);
    }
}
