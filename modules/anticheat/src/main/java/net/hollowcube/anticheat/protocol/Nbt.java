package net.hollowcube.anticheat.protocol;

import java.nio.charset.StandardCharsets;

/// The sliver of NBT the capture needs: enough to walk a tag in network form and find where it
/// ends.
///
/// Nothing in phase 0 interprets NBT — the packets that embed it keep it verbatim — so this is a
/// length walk rather than a parser. Network form is what `NbtIo#readAnyTag` reads: a type byte
/// then the payload, with no root name.
public final class Nbt {

    public static final int TAG_END = 0;
    public static final int TAG_BYTE = 1;
    public static final int TAG_SHORT = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_BYTE_ARRAY = 7;
    public static final int TAG_STRING = 8;
    public static final int TAG_LIST = 9;
    public static final int TAG_COMPOUND = 10;
    public static final int TAG_INT_ARRAY = 11;
    public static final int TAG_LONG_ARRAY = 12;

    /// Skips one whole tag, type byte included.
    public static void skip(ByteReader reader) {
        int type = reader.u8();
        if (type != TAG_END) skipPayload(reader, type);
    }

    /// Skips the payload of a tag whose type byte the caller has already read, which is what a
    /// walk over a compound's entries needs.
    public static void skipPayload(ByteReader reader, int type) {
        switch (type) {
            case TAG_BYTE -> reader.skip(1);
            case TAG_SHORT -> reader.skip(2);
            case TAG_INT, TAG_FLOAT -> reader.skip(4);
            case TAG_LONG, TAG_DOUBLE -> reader.skip(8);
            case TAG_BYTE_ARRAY -> reader.skip(arrayLength(reader));
            case TAG_STRING -> reader.skip(reader.u16());
            case TAG_LIST -> {
                int elementType = reader.u8();
                int length = arrayLength(reader);
                if (elementType == TAG_END && length > 0)
                    throw new ProtocolException("non-empty list of end tags");
                for (int i = 0; i < length; i++) skipPayload(reader, elementType);
            }
            case TAG_COMPOUND -> {
                int entryType;
                while ((entryType = reader.u8()) != TAG_END) {
                    name(reader);
                    skipPayload(reader, entryType);
                }
            }
            case TAG_INT_ARRAY -> reader.skip(arrayLength(reader) * 4);
            case TAG_LONG_ARRAY -> reader.skip(arrayLength(reader) * 8);
            default -> throw new ProtocolException("unknown nbt tag type: " + type);
        }
    }

    /// A compound entry's name: modified utf-8 behind a u16 length. Every name in a vanilla
    /// registry is ascii, so reading the bytes as utf-8 is the same string.
    public static String name(ByteReader reader) {
        return new String(reader.bytes(reader.u16()), StandardCharsets.UTF_8);
    }

    /// Bounded by what is left in the frame, so a corrupt length can only fail here rather than
    /// walk off the end of the tag.
    private static int arrayLength(ByteReader reader) {
        int length = reader.i32();
        if (length < 0 || length > reader.remaining()) throw new ProtocolException("nbt array too big: " + length);
        return length;
    }

    private Nbt() {}
}
