package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// Splits a `set_entity_data` payload into its entries, so the entity table can keep a last-wins
/// metadata map per entity — pose, the shared flags, an interaction entity's width and height are
/// all metadata, and merging needs entry boundaries.
///
/// The wire is `u8 index (0xFF ends) | varint serializerId | value`, with the value's length
/// decided by the serializer (`EntityDataSerializers` registration order = the id). Only shapes
/// verified against the 26.2 decompile are walked; the first entry whose serializer this cannot
/// walk (item stacks, particles, the data-driven variant holders) ends the split, and whatever
/// was walked before it still merges — best effort, never a guess that would misread every entry
/// after a wrong length.
public final class Metadata776 {

    /// One entry, `bytes` being the whole thing (index, serializer id and value), so a merged map
    /// re-encodes by concatenation plus the terminator.
    public record Entry(int index, int serializerId, byte[] bytes) {}

    public static final int TERMINATOR = 0xFF;

    // EntityDataSerializers registration order.
    private static final int BYTE = 0;
    private static final int INT = 1;
    private static final int LONG = 2;
    private static final int FLOAT = 3;
    private static final int STRING = 4;
    private static final int COMPONENT = 5;
    private static final int OPTIONAL_COMPONENT = 6;
    private static final int BOOLEAN = 8;
    private static final int ROTATIONS = 9;
    private static final int BLOCK_POS = 10;
    private static final int OPTIONAL_BLOCK_POS = 11;
    private static final int DIRECTION = 12;
    private static final int OPTIONAL_LIVING_ENTITY_REFERENCE = 13;
    private static final int BLOCK_STATE = 14;
    private static final int OPTIONAL_BLOCK_STATE = 15;
    private static final int OPTIONAL_UNSIGNED_INT = 19;
    private static final int POSE = 20;
    private static final int OPTIONAL_GLOBAL_POS = 33;
    private static final int VECTOR3 = 39;
    private static final int QUATERNION = 40;

    /// The entries this could walk, in payload order; empty when the payload opens with something
    /// unwalkable. A short or malformed payload yields what was whole before the cut.
    public static List<Entry> entries(byte[] payload) {
        var entries = new ArrayList<Entry>();
        var reader = new ByteReader(payload);
        try {
            while (reader.remaining() > 0) {
                int start = reader.index();
                int index = reader.u8();
                if (index == TERMINATOR) break;
                int serializerId = reader.varInt();
                if (!skipValue(reader, serializerId)) break;
                entries.add(new Entry(index, serializerId, reader.since(start)));
            }
        } catch (ProtocolException _) {
            // The cut entry is not added; everything before it is still whole.
        }
        return entries;
    }

    private static boolean skipValue(ByteReader reader, int serializerId) {
        switch (serializerId) {
            case BYTE, BOOLEAN -> reader.skip(1);
            case INT, DIRECTION, BLOCK_STATE, OPTIONAL_BLOCK_STATE, OPTIONAL_UNSIGNED_INT, POSE -> reader.varInt();
            case LONG -> reader.varLong();
            case FLOAT -> reader.skip(4);
            case STRING -> reader.utf();
            case COMPONENT -> reader.skipNbt();
            case OPTIONAL_COMPONENT -> {
                if (reader.bool()) reader.skipNbt();
            }
            case ROTATIONS, VECTOR3 -> reader.skip(12);
            case BLOCK_POS -> reader.skip(8);
            case OPTIONAL_BLOCK_POS -> {
                if (reader.bool()) reader.skip(8);
            }
            case OPTIONAL_LIVING_ENTITY_REFERENCE -> {
                if (reader.bool()) reader.skip(16);
            }
            case OPTIONAL_GLOBAL_POS -> {
                if (reader.bool()) {
                    reader.utf();
                    reader.skip(8);
                }
            }
            case QUATERNION -> reader.skip(16);
            default -> {
                return false;
            }
        }
        return true;
    }

    private Metadata776() {}
}
