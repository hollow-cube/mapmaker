package net.hollowcube.common.util.font;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

public class FontWidths {

    public static final Int2IntMap CURRENCY_WIDTHS = new Int2IntArrayMap();
    static {
        CURRENCY_WIDTHS.put('1', 4);
        CURRENCY_WIDTHS.put('2', 5);
        CURRENCY_WIDTHS.put('3', 5);
        CURRENCY_WIDTHS.put('4', 5);
        CURRENCY_WIDTHS.put('5', 5);
        CURRENCY_WIDTHS.put('6', 5);
        CURRENCY_WIDTHS.put('7', 5);
        CURRENCY_WIDTHS.put('8', 5);
        CURRENCY_WIDTHS.put('9', 5);
        CURRENCY_WIDTHS.put('0', 5);
        CURRENCY_WIDTHS.put('k', 5);
        CURRENCY_WIDTHS.put('m', 6);
        CURRENCY_WIDTHS.put('b', 5);
        CURRENCY_WIDTHS.put('.', 2);
        CURRENCY_WIDTHS.put('c', 6);
    }

    public static final Int2IntMap SMALL_WIDTHS = new Int2IntArrayMap();
    static {
        for (char c = 'a'; c <= 'z'; c++) {
            SMALL_WIDTHS.put(c, 6);
        }
        SMALL_WIDTHS.put('i', 4);
    }

    public static final Int2IntMap SMALL_TALL_WIDTHS = new Int2IntArrayMap();
    static {
        for (char c = 'a'; c <= 'z'; c++) {
            SMALL_TALL_WIDTHS.put(c, 6);
        }
        SMALL_TALL_WIDTHS.put('i', 4);
        SMALL_TALL_WIDTHS.put('1', 4);
        SMALL_TALL_WIDTHS.put('2', 5);
        SMALL_TALL_WIDTHS.put('3', 5);
        SMALL_TALL_WIDTHS.put('4', 5);
        SMALL_TALL_WIDTHS.put('5', 5);
        SMALL_TALL_WIDTHS.put('6', 5);
        SMALL_TALL_WIDTHS.put('7', 5);
        SMALL_TALL_WIDTHS.put('8', 5);
        SMALL_TALL_WIDTHS.put('9', 5);
        SMALL_TALL_WIDTHS.put('0', 5);
        SMALL_TALL_WIDTHS.put('/', 4);
        SMALL_TALL_WIDTHS.put('-', 3);
        SMALL_TALL_WIDTHS.put('.', 2);
    }

    public static final Int2IntMap SMALL_CAPS_WIDTHS = new Int2IntArrayMap();
    static {
        SMALL_CAPS_WIDTHS.put('ᴀ', 6);
        SMALL_CAPS_WIDTHS.put('ʙ', 6);
        SMALL_CAPS_WIDTHS.put('ᴄ', 6);
        SMALL_CAPS_WIDTHS.put('ᴅ', 6);
        SMALL_CAPS_WIDTHS.put('ᴇ', 6);
        SMALL_CAPS_WIDTHS.put('ꜰ', 6);
        SMALL_CAPS_WIDTHS.put('ɢ', 6);
        SMALL_CAPS_WIDTHS.put('ʜ', 6);
        SMALL_CAPS_WIDTHS.put('ɪ', 4);
        SMALL_CAPS_WIDTHS.put('ᴊ', 6);
        SMALL_CAPS_WIDTHS.put('ᴋ', 6);
        SMALL_CAPS_WIDTHS.put('ʟ', 6);
        SMALL_CAPS_WIDTHS.put('ᴍ', 6);
        SMALL_CAPS_WIDTHS.put('ɴ', 6);
        SMALL_CAPS_WIDTHS.put('ᴏ', 6);
        SMALL_CAPS_WIDTHS.put('ᴘ', 6);
        SMALL_CAPS_WIDTHS.put('ǫ', 6);
        SMALL_CAPS_WIDTHS.put('ʀ', 6);
        SMALL_CAPS_WIDTHS.put('ѕ', 6);
        SMALL_CAPS_WIDTHS.put('ᴛ', 6);
        SMALL_CAPS_WIDTHS.put('ᴜ', 6);
        SMALL_CAPS_WIDTHS.put('ᴠ', 6);
        SMALL_CAPS_WIDTHS.put('ᴡ', 6);
        SMALL_CAPS_WIDTHS.put('х', 6);
        SMALL_CAPS_WIDTHS.put('ʏ', 6);
        SMALL_CAPS_WIDTHS.put('ᴢ', 6);
    }
}
