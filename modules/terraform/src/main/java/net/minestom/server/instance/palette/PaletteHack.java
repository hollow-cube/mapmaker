package net.minestom.server.instance.palette;

import net.hollowcube.terraform.util.HeapSize;
import org.jetbrains.annotations.ApiStatus;

/// Internal theft of [PaletteImpl] for memory accounting.
@ApiStatus.Internal
public final class PaletteHack {

    private static final long PALETTE_OBJECT_BYTES = 40;
    private static final long INT_ARRAY_LIST_BYTES = 24;

    public static long residentBytes(Palette palette) {
        var impl = (PaletteImpl) palette;
        long total = PALETTE_OBJECT_BYTES;

        long[] values = impl.values;
        if (values != null) {
            total += HeapSize.array(values.length, Long.BYTES);
        }

        var list = impl.paletteToValueList;
        if (list != null) {
            total += INT_ARRAY_LIST_BYTES + HeapSize.array(list.elements().length, Integer.BYTES);
        }

        var map = impl.valueToPaletteMap;
        if (map != null) {
            total += HeapSize.openHashMap(map.size(), Integer.BYTES, Integer.BYTES);
        }

        return total;
    }

    private PaletteHack() {}
}
