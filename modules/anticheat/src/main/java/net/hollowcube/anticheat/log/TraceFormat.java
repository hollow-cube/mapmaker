package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/// The on-disk shape of a capture trace, and the primitives [TraceWriter] and [TraceReader]
/// share.
///
/// A file is a fixed head, a reserved header region and a zstd stream:
/// ```
/// "HCTR" | u16 formatVersion | u32 headerCapacity | u32 headerLength
/// headerCapacity bytes: headerLength bytes of UTF-8 JSON ([TraceHeader]), then zero padding
/// zstd stream: prelude frames | world chunks | frames
/// ```
///
/// The header is written first, into a region large enough for the values it grows into, and
/// rewritten in place at close. Writing it last with a trailer would compress better and never
/// overflow, but a file cut short by a crash would then have no header at all; this way a cut file
/// still parses, keeps its opening header, and yields every frame up to the last [TraceWriter#flush()].
///
/// Everything inside the zstd stream is big-endian, with varints where vanilla uses them.
public final class TraceFormat {

    /// `HCTR`, "hollow cube trace".
    public static final int MAGIC = 0x48435452;

    /// Bumped by any incompatible change to the container, the header or the body. Readers are
    /// expected to keep handling every version they ever wrote (see the replay data version
    /// discipline); version 1 is the one exception phase 0 allows itself, because its world section
    /// also carried heightmaps and a block-entity/light tail that nothing replays.
    public static final int VERSION_LATEST = 2;

    /// Level 3, the same trade the replay recorder makes on its hot path: traces are written on
    /// a proxy under a live connection, not offline.
    public static final int COMPRESSION_LEVEL = 3;

    /// magic + version + capacity + length.
    static final int FIXED_HEAD_LENGTH = 14;

    /// Reserved on top of the opening header, so the fields only known at close (`endedAt`,
    /// `closedBy`, flags, counters) can be rewritten in place.
    static final int HEADER_SLACK = 1024;

    static final int MAX_HEADER_LENGTH = 1 << 20;

    /// A corruption bound, not a protocol one: chunk and registry frames are the large ones and
    /// sit far below the 16 MiB here, so a length past it is a bad varint rather than a big packet.
    static final int MAX_BYTES_LENGTH = 1 << 24;

    static final int SECTION_INLINE = 0;
    static final int SECTION_BY_HASH = 1;

    /// A SHA-256 digest, for the content-addressed section store the format reserves but does not
    /// yet write.
    public static final int SECTION_HASH_LENGTH = 32;

    static void writeVarInt(DataOutput out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    static void writeVarLong(DataOutput out, long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            out.writeByte((int) (value & 0x7F | 0x80));
            value >>>= 7;
        }
        out.writeByte((int) value);
    }

    static long readVarLong(DataInput in) throws IOException {
        long result = 0;
        int bytes = 0;
        byte read;
        do {
            read = in.readByte();
            result |= (long) (read & 0x7F) << bytes++ * 7;
            if (bytes > 10) throw new TraceFormatException("varlong too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    static int readVarInt(DataInput in) throws IOException {
        int result = 0;
        int bytes = 0;
        byte read;
        do {
            read = in.readByte();
            result |= (read & 0x7F) << bytes++ * 7;
            if (bytes > 5) throw new TraceFormatException("varint too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    static void writeBytes(DataOutput out, byte[] value) throws IOException {
        writeVarInt(out, value.length);
        out.write(value);
    }

    static byte[] readBytes(DataInput in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > MAX_BYTES_LENGTH)
            throw new TraceFormatException("byte array length out of range: " + length);
        var value = new byte[length];
        in.readFully(value);
        return value;
    }

    /// Stable wire codes, deliberately not the enum ordinals: the protocol enums are free to grow
    /// or reorder without silently reinterpreting every frame already on disk.
    static int directionCode(Direction direction) {
        return switch (direction) {
            case C2S -> 0;
            case S2C -> 1;
        };
    }

    static Direction direction(int code) {
        return switch (code) {
            case 0 -> Direction.C2S;
            case 1 -> Direction.S2C;
            default -> throw new TraceFormatException("unknown direction: " + code);
        };
    }

    static int stateCode(ProtocolState state) {
        return switch (state) {
            case HANDSHAKE -> 0;
            case LOGIN -> 1;
            case CONFIGURATION -> 2;
            case PLAY -> 3;
        };
    }

    static ProtocolState state(int code) {
        return switch (code) {
            case 0 -> ProtocolState.HANDSHAKE;
            case 1 -> ProtocolState.LOGIN;
            case 2 -> ProtocolState.CONFIGURATION;
            case 3 -> ProtocolState.PLAY;
            default -> throw new TraceFormatException("unknown protocol state: " + code);
        };
    }

    private TraceFormat() {}
}
