package net.hollowcube.terraform.nbt;

import net.kyori.adventure.nbt.*;

import java.io.*;

import static net.hollowcube.terraform.nbt.NbtTypes.*;

/// A pull-based NBT reader.
public final class NbtStreamReader implements Closeable {

    public static final int MAX_DEPTH = 512;
    public static final long MAX_VALUE_BYTES = 128L << 20;
    private static final int TAG_OVERHEAD_BYTES = 64;

    private final DataInputStream in;
    private int depth;
    private long valueBytes;

    public NbtStreamReader(InputStream in) {
        // Buffered so per-tag reads and the chunked block-data pulls don't hit the gzip stream per byte.
        this.in = new DataInputStream(new BufferedInputStream(in, 1 << 16));
    }

    public byte readType() throws IOException {
        return in.readByte();
    }

    public String readString() throws IOException {
        return readUTF();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    /// Any numeric payload (`BYTE` through `DOUBLE`) as its boxed value. Throws for any other type.
    public Number readNumber(byte type) throws IOException {
        return switch (type) {
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            default -> throw new IOException("expected a numeric tag, got type " + type);
        };
    }

    public int[] readIntArray() throws IOException {
        int n = readLength();
        count(TAG_OVERHEAD_BYTES + 4L * n);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = in.readInt();
        return out;
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        in.readFully(buf, off, len);
    }

    public void skipBytes(long n) throws IOException {
        in.skipNBytes(n);
    }

    /// The length of a list whose element type `elem` was just read. An unknown element type, or a
    /// non-empty list of `END` elements (each element would cost no input), is malformed, so it throws.
    public int readListLength(byte elem) throws IOException {
        if (elem < END || elem > LONG_ARRAY)
            throw new IOException("unknown NBT list element type " + elem);
        int len = readLength();
        if (elem == END && len > 0)
            throw new IOException("non-empty NBT list of END elements (length " + len + ")");
        return len;
    }

    public void skipPayload(byte type) throws IOException {
        if (depth >= MAX_DEPTH)
            throw new IOException("NBT nested too deeply (limit " + MAX_DEPTH + ")");
        depth++;
        try {
            skipPayload0(type);
        } finally {
            depth--;
        }
    }

    private void skipPayload0(byte type) throws IOException {
        switch (type) {
            case BYTE -> in.skipNBytes(1);
            case SHORT -> in.skipNBytes(2);
            case INT, FLOAT -> in.skipNBytes(4);
            case LONG, DOUBLE -> in.skipNBytes(8);
            case BYTE_ARRAY -> skipBytes(readLength());
            case STRING -> in.skipNBytes(in.readUnsignedShort());
            case INT_ARRAY -> skipBytes((long) readLength() * 4);
            case LONG_ARRAY -> skipBytes((long) readLength() * 8);
            case LIST -> {
                byte elem = readType();
                int len = readListLength(elem);
                int width = fixedWidth(elem);
                if (width > 0) {
                    skipBytes((long) len * width);
                } else {
                    for (int i = 0; i < len; i++) skipPayload(elem);
                }
            }
            case COMPOUND -> {
                byte t;
                while ((t = readType()) != END) {
                    in.skipNBytes(in.readUnsignedShort()); // name
                    skipPayload(t);
                }
            }
            case END -> {
                /* nothing */
            }
            default -> throw new IOException("unknown NBT tag type " + type);
        }
    }

    public BinaryTag readPayload(byte type) throws IOException {
        if (depth >= MAX_DEPTH)
            throw new IOException("NBT nested too deeply (limit " + MAX_DEPTH + ")");
        count(TAG_OVERHEAD_BYTES);
        depth++;
        try {
            return readPayload0(type);
        } finally {
            depth--;
        }
    }

    private BinaryTag readPayload0(byte type) throws IOException {
        return switch (type) {
            case BYTE -> ByteBinaryTag.byteBinaryTag(in.readByte());
            case SHORT -> ShortBinaryTag.shortBinaryTag(in.readShort());
            case INT -> IntBinaryTag.intBinaryTag(in.readInt());
            case LONG -> LongBinaryTag.longBinaryTag(in.readLong());
            case FLOAT -> FloatBinaryTag.floatBinaryTag(in.readFloat());
            case DOUBLE -> DoubleBinaryTag.doubleBinaryTag(in.readDouble());
            case BYTE_ARRAY -> {
                int n = readLength();
                count(n);
                byte[] b = new byte[n];
                in.readFully(b);
                yield ByteArrayBinaryTag.byteArrayBinaryTag(b);
            }
            case STRING -> StringBinaryTag.stringBinaryTag(readUTF());
            case INT_ARRAY -> IntArrayBinaryTag.intArrayBinaryTag(readIntArray());
            case LONG_ARRAY -> {
                int n = readLength();
                count(8L * n);
                long[] l = new long[n];
                for (int i = 0; i < n; i++) l[i] = in.readLong();
                yield LongArrayBinaryTag.longArrayBinaryTag(l);
            }
            case LIST -> {
                byte elem = readType();
                int len = readListLength(elem);
                ListBinaryTag.Builder<BinaryTag> b = ListBinaryTag.builder();
                for (int i = 0; i < len; i++) b.add(readPayload(elem));
                yield b.build();
            }
            case COMPOUND -> {
                CompoundBinaryTag.Builder b = CompoundBinaryTag.builder();
                byte t;
                while ((t = readType()) != END) {
                    String name = readString();
                    b.put(name, readPayload(t));
                }
                yield b.build();
            }
            default -> throw new IOException("unknown NBT tag type " + type);
        };
    }

    private String readUTF() throws IOException {
        String value = in.readUTF();
        count(TAG_OVERHEAD_BYTES + 2L * value.length());
        return value;
    }

    private void count(long bytes) throws IOException {
        if (bytes > MAX_VALUE_BYTES - valueBytes) {
            throw new IOException("NBT values exceed the limit of " + MAX_VALUE_BYTES + " bytes");
        }
        valueBytes += bytes;
    }

    public int readLength() throws IOException {
        int n = in.readInt();
        if (n < 0) throw new IOException("negative NBT array length " + n);
        return n;
    }

    /// The payload size of a fixed-width scalar tag type, or 0 for a variable-width one.
    private static int fixedWidth(byte type) {
        return switch (type) {
            case BYTE -> 1;
            case SHORT -> 2;
            case INT, FLOAT -> 4;
            case LONG, DOUBLE -> 8;
            default -> 0;
        };
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
