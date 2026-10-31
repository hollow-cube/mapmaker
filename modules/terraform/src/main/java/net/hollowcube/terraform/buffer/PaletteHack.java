package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.hollowcube.terraform.util.HeapSize;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.instance.palette.Palettes;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.IntPredicate;

/// Internal theft of Minestom's `PaletteImpl` for memory accounting and fastpath bulk actions.
///
/// Needs `--add-opens net.minestom.server/net.minestom.server.instance.palette` under JPMS.
final class PaletteHack {
    private static final long PALETTE_OBJECT_BYTES = 40;
    private static final long INT_ARRAY_LIST_BYTES = 24;

    private static final VarHandle MIN_BITS_PER_ENTRY;
    private static final VarHandle MAX_BITS_PER_ENTRY;
    private static final VarHandle DIRECT_BITS;
    private static final VarHandle BITS_PER_ENTRY;
    private static final VarHandle COUNT;
    private static final VarHandle VALUES;
    private static final VarHandle PALETTE_TO_VALUE_LIST;
    private static final VarHandle VALUE_TO_PALETTE_MAP;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Palette.class, MethodHandles.lookup());
            Class<?> impl = lookup.findClass("net.minestom.server.instance.palette.PaletteImpl");
            MIN_BITS_PER_ENTRY = lookup.findVarHandle(impl, "minBitsPerEntry", byte.class);
            MAX_BITS_PER_ENTRY = lookup.findVarHandle(impl, "maxBitsPerEntry", byte.class);
            DIRECT_BITS = lookup.findVarHandle(impl, "directBits", byte.class);
            BITS_PER_ENTRY = lookup.findVarHandle(impl, "bitsPerEntry", byte.class);
            COUNT = lookup.findVarHandle(impl, "count", int.class);
            VALUES = lookup.findVarHandle(impl, "values", long[].class);
            PALETTE_TO_VALUE_LIST = lookup.findVarHandle(
                impl,
                "paletteToValueList",
                IntArrayList.class
            );
            VALUE_TO_PALETTE_MAP = lookup.findVarHandle(
                impl,
                "valueToPaletteMap",
                Int2IntOpenHashMap.class
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static long residentBytes(Palette palette) {
        long total = PALETTE_OBJECT_BYTES;

        long[] values = palette.indexedValues();
        if (values != null) {
            total += HeapSize.array(values.length, Long.BYTES);
        }

        var list = dictionary(palette);
        if (list != null) {
            total += INT_ARRAY_LIST_BYTES + HeapSize.array(list.elements().length, Integer.BYTES);
        }

        var map = (Int2IntOpenHashMap) VALUE_TO_PALETTE_MAP.get(palette);
        if (map != null) {
            total += HeapSize.openHashMap(map.size(), Integer.BYTES, Integer.BYTES);
        }

        return total;
    }

    /// Whether any value the palette may hold matches. Checks the dictionary rather than every cell, so a
    /// stale dictionary entry no cell references can report a false positive, never a false negative.
    static boolean contains(Palette palette, IntPredicate predicate) {
        if (palette.bitsPerEntry() == 0) return predicate.test(palette.singleValue());

        var dictionary = dictionary(palette);
        if (dictionary != null) {
            for (int i = 0; i < dictionary.size(); i++) {
                if (predicate.test(dictionary.getInt(i))) return true;
            }
            return false;
        }

        boolean[] found = {false};
        palette.getAll((_, _, _, value) -> {
            if (!found[0] && predicate.test(value)) found[0] = true;
        });
        return found[0];
    }

    /// Returns a copy of the palette with all entries offset by 1.
    static Palette copyWithOffset(Palette from) {
        var result = Palette.empty(
            from.dimension(),
            (byte) MIN_BITS_PER_ENTRY.get(from),
            (byte) MAX_BITS_PER_ENTRY.get(from),
            (byte) DIRECT_BITS.get(from)
        );
        copy(from, result, 1);
        return result;
    }

    /// Replace a world palette with a previously 1-offset palette
    static void applyFromOffset(Palette edits, Palette to) {
        copy(edits, to, -1);
    }

    /// The palette's dictionary of values, or null in direct mode.
    static @Nullable IntArrayList dictionary(Palette palette) {
        return (IntArrayList) PALETTE_TO_VALUE_LIST.get(palette);
    }

    /// Very gross fast cases :(
    ///
    /// Some version probably should be partially upstreamed at some point
    private static void copy(Palette source, Palette target, int offset) {
        if (source.dimension() != target.dimension())
            throw new IllegalArgumentException("palette dimensions must match");
        if (offset == -1 && source.count() != source.maxSize())
            throw new IllegalArgumentException("offset apply requires full coverage");
        int sourceBits = source.bitsPerEntry();
        int targetDirectBits = (byte) DIRECT_BITS.get(target);
        if (sourceBits == 0 || source.count() == 0) {
            int value = sourceBits == 0 ? source.singleValue() : 0;
            target.fill(translate(value, offset, targetDirectBits));
            return;
        }

        long[] sourceValues = Objects.requireNonNull(source.indexedValues());
        if (sourceBits > (byte) MAX_BITS_PER_ENTRY.get(source)) {
            // Direct cells already hold state ids: translate the packed fields in one pass, without the
            // temporary int[4096] and clone Palette.offset/replaceAll would use.
            int[] count = {0};
            long[] values = Palettes.remap(
                source.dimension(),
                sourceBits,
                targetDirectBits,
                sourceValues,
                value -> {
                    int translated = translate(value, offset, targetDirectBits);
                    if (translated != 0) count[0]++;
                    return translated;
                }
            );
            store(target, targetDirectBits, values, null, null, count[0]);
            return;
        }

        // Palette.offset forces indirect palettes into direct storage. Translate the dictionary instead,
        // keeping the cell indices and their bit width whenever possible.
        var dictionary = Objects.requireNonNull(dictionary(source)).clone();
        var reverse = new Int2IntOpenHashMap(dictionary.size());
        reverse.defaultReturnValue(-1);
        int bits = Math.max(sourceBits, (byte) MIN_BITS_PER_ENTRY.get(target));
        boolean direct = bits > (byte) MAX_BITS_PER_ENTRY.get(target);
        if (direct) bits = targetDirectBits;
        long[] values = bits == sourceBits
            ? sourceValues.clone()
            : Palettes.remap(source.dimension(), sourceBits, bits, sourceValues, index -> index);

        // Translate and deduplicate together. Full coverage leaves the leave entries unused, so remove them
        // by moving the last dictionary slot into the hole and repairing its indices.
        int zeroCount = 0;
        for (int index = 0; index < dictionary.size();) {
            int value = dictionary.getInt(index);
            int translated = offset == -1 && value == 0
                ? -1
                : translate(value, offset, targetDirectBits);
            if (translated == 0)
                zeroCount += Palettes.countEquals(bits, values, source.maxSize(), index);

            int existing = reverse.get(translated);
            if (translated < 0 || existing >= 0) {
                if (existing >= 0)
                    Palettes.replaceEquals(bits, values, source.maxSize(), index, existing);
                int last = dictionary.size() - 1;
                int moved = dictionary.removeInt(last);
                if (index < last) {
                    dictionary.set(index, moved);
                    Palettes.replaceEquals(bits, values, source.maxSize(), last, index);
                }
            } else {
                dictionary.set(index, translated);
                reverse.put(translated, index);
                index++;
            }
        }

        if (zeroCount == source.maxSize()) {
            target.fill(0);
            return;
        }
        if (direct)
            values = Palettes.remap(source.dimension(), bits, bits, values, dictionary::getInt);
        store(
            target,
            bits,
            values,
            direct ? null : dictionary,
            direct ? null : reverse,
            source.maxSize() - zeroCount
        );
    }

    private static void store(
        Palette target,
        int bits,
        long[] values,
        @Nullable IntArrayList dictionary,
        @Nullable Int2IntOpenHashMap reverse,
        int count
    ) {
        BITS_PER_ENTRY.set(target, (byte) bits);
        VALUES.set(target, values);
        PALETTE_TO_VALUE_LIST.set(target, dictionary);
        VALUE_TO_PALETTE_MAP.set(target, reverse);
        COUNT.set(target, count);
    }

    private static int translate(int value, int offset, int directBits) {
        long translated = (long) value + offset;
        if (translated < 0 || translated >= (1L << directBits))
            throw new IllegalArgumentException(
                "offset block value exceeds palette range: " + translated
            );
        return (int) translated;
    }

    private PaletteHack() {}
}
