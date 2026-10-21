package net.hollowcube.terraform.util;

import it.unimi.dsi.fastutil.HashCommon;
import net.kyori.adventure.nbt.*;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/// Retained-heap estimates for various types used for memory budgeting.
///
/// Estimates a 64-bit HotSpot with compressed oops. Over-estimates a little on JEP-519 or native image.
/// (12-byte object headers, 16-byte array headers, 4-byte references, 8-byte alignment)
public final class HeapSize {
    private static final long OPEN_HASH_MAP = 64;
    private static final long BLOCK_ENTITY = 64; // Block entity overhead (excluding nbt)

    public static final long SECTION = 40 + 24; // Section overhead

    public static long array(int length, int elementBytes) {
        return align(16L + (long) length * elementBytes);
    }

    /// Fastutil XOpenHashMap object
    public static long openHashMap(int size, int keyBytes, int valueBytes) {
        int table = Math.max(32, HashCommon.arraySize(size + 1, 0.75f)) + 1;
        return OPEN_HASH_MAP + array(table, keyBytes) + array(table, valueBytes);
    }

    public static long nbt(@Nullable BinaryTag tag) {
        if (tag == null) return 0;
        return switch (tag) {
            case CompoundBinaryTag compound -> {
                long total = 16; // map object
                for (Map.Entry<String, ? extends BinaryTag> entry : compound) {
                    total += 16 + 2L * entry.getKey().length() + nbt(entry.getValue());
                }
                yield total;
            }
            case ListBinaryTag list -> {
                long total = 16;
                for (BinaryTag element : list) total += nbt(element);
                yield total;
            }
            case StringBinaryTag string -> 16 + 2L * string.value().length();
            case ByteArrayBinaryTag array -> 16 + array.size();
            case IntArrayBinaryTag array -> 16 + 4L * array.size();
            case LongArrayBinaryTag array -> 16 + 8L * array.size();
            default -> 8; // byte/short/int/long/float/double
        };
    }

    public static long block(Block block) {
        return BLOCK_ENTITY + nbt(block.nbt());
    }

    private static long align(long bytes) {
        return (bytes + 7) & ~7L;
    }

    private HeapSize() {}
}
