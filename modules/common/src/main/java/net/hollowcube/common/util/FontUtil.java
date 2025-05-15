package net.hollowcube.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FontUtil {
    private static final Logger logger = LoggerFactory.getLogger(FontUtil.class);

    private FontUtil() {
    }

    public static final Map<Integer, Integer> GLYPH_WIDTHS = Map.<Integer, Integer>ofEntries(
            Map.entry(65, 6), Map.entry(196, 6), Map.entry(197, 6), Map.entry(198, 6),
            Map.entry(66, 6), Map.entry(67, 6), Map.entry(199, 6), Map.entry(68, 6),
            Map.entry(69, 6), Map.entry(70, 6), Map.entry(71, 6), Map.entry(72, 6),
            Map.entry(73, 4), Map.entry(74, 6), Map.entry(75, 6), Map.entry(76, 6),
            Map.entry(77, 6), Map.entry(78, 6), Map.entry(209, 6), Map.entry(79, 6),
            Map.entry(216, 6), Map.entry(80, 6), Map.entry(81, 6), Map.entry(82, 6),
            Map.entry(83, 6), Map.entry(84, 6), Map.entry(85, 6), Map.entry(220, 6),
            Map.entry(86, 6), Map.entry(87, 6), Map.entry(88, 6), Map.entry(89, 6),
            Map.entry(90, 6), Map.entry(97, 6), Map.entry(225, 6), Map.entry(226, 6),
            Map.entry(224, 6), Map.entry(229, 6), Map.entry(230, 6), Map.entry(98, 6),
            Map.entry(99, 6), Map.entry(231, 6), Map.entry(100, 6), Map.entry(101, 6),
            Map.entry(234, 6), Map.entry(235, 6), Map.entry(102, 5), Map.entry(103, 6),
            Map.entry(104, 6), Map.entry(105, 2), Map.entry(237, 3), Map.entry(238, 4),
            Map.entry(106, 6), Map.entry(107, 5), Map.entry(108, 3), Map.entry(109, 6),
            Map.entry(110, 6), Map.entry(241, 6), Map.entry(111, 6), Map.entry(243, 6),
            Map.entry(244, 6), Map.entry(246, 6), Map.entry(242, 6), Map.entry(248, 6),
            Map.entry(112, 6), Map.entry(113, 6), Map.entry(114, 6), Map.entry(115, 6),
            Map.entry(116, 4), Map.entry(117, 6), Map.entry(250, 6), Map.entry(251, 6),
            Map.entry(252, 6), Map.entry(249, 6), Map.entry(118, 6), Map.entry(119, 6),
            Map.entry(120, 6), Map.entry(121, 6), Map.entry(255, 6), Map.entry(122, 6),
            Map.entry(937, 6), Map.entry(48, 6), Map.entry(49, 6), Map.entry(50, 6),
            Map.entry(51, 6), Map.entry(52, 6), Map.entry(53, 6), Map.entry(54, 6),
            Map.entry(55, 6), Map.entry(56, 6), Map.entry(57, 6), Map.entry(46, 2),
            Map.entry(44, 2), Map.entry(58, 2), Map.entry(59, 2), Map.entry(33, 2),
            Map.entry(161, 2), Map.entry(63, 6), Map.entry(191, 6), Map.entry(183, 3),
            Map.entry(8226, 3), Map.entry(42, 4), Map.entry(35, 6), Map.entry(47, 6),
            Map.entry(92, 6), Map.entry(40, 4), Map.entry(41, 4), Map.entry(123, 5),
            Map.entry(125, 5), Map.entry(91, 4), Map.entry(93, 4), Map.entry(45, 6),
            Map.entry(173, 3), Map.entry(8212, 8), Map.entry(95, 6), Map.entry(171, 7),
            Map.entry(187, 6), Map.entry(34, 4), Map.entry(39, 2), Map.entry(32, 4),
            Map.entry(162, 5), Map.entry(164, 4), Map.entry(36, 6), Map.entry(402, 6),
            Map.entry(163, 6), Map.entry(43, 6), Map.entry(247, 6), Map.entry(61, 6),
            Map.entry(62, 5), Map.entry(60, 5), Map.entry(177, 6), Map.entry(8776, 8),
            Map.entry(126, 7), Map.entry(172, 6), Map.entry(8734, 8), Map.entry(8721, 6),
            Map.entry(8730, 9), Map.entry(37, 6), Map.entry(9786, 9), Map.entry(9787, 9),
            Map.entry(9788, 9), Map.entry(9792, 6), Map.entry(9794, 8), Map.entry(9824, 8),
            Map.entry(9827, 8), Map.entry(9829, 8), Map.entry(9830, 8), Map.entry(64, 7),
            Map.entry(38, 6), Map.entry(182, 7), Map.entry(167, 6), Map.entry(174, 7),
            Map.entry(124, 2), Map.entry(94, 6), Map.entry(8962, 6), Map.entry(184, 4),
            Map.entry(8230, 5), Map.entry(176, 5)
    );

    public static final Map<String, Int2IntMap> GLYPH_WIDTHS_V2;
    private static final Int2IntMap ALL_GLYPH_WIDTHS_V2;

    static {
        var result = new HashMap<String, Int2IntMap>();

        var defaultGlyphs = new Int2IntArrayMap();
        defaultGlyphs.putAll(GLYPH_WIDTHS);
        result.put("ascii", defaultGlyphs);
        result.put("line_0", defaultGlyphs);
        result.put("line_1", defaultGlyphs);
        result.put("line_2", defaultGlyphs);
        result.put("line_3", defaultGlyphs);
        result.put("line_3_1", defaultGlyphs);
        result.put("line_4", defaultGlyphs);
        result.put("line_4_1", defaultGlyphs);
        result.put("bossbar_ascii_1", defaultGlyphs);

        var currencyGlyphs = new Int2IntArrayMap();
        currencyGlyphs.putAll(Map.<Integer, Integer>ofEntries(
//                Map.entry((int) '1', 0), Map.entry((int) '2', 0),
//                Map.entry((int) '3', 0), Map.entry((int) '4', 0)
                Map.entry((int) '1', 4), Map.entry((int) '2', 5),
                Map.entry((int) '3', 5), Map.entry((int) '4', 5),
                Map.entry((int) '5', 5), Map.entry((int) '6', 5),
                Map.entry((int) '7', 5), Map.entry((int) '8', 5),
                Map.entry((int) '9', 5), Map.entry((int) '0', 5),
                Map.entry((int) 'k', 5), Map.entry((int) 'm', 6),
                Map.entry((int) 'b', 5), Map.entry((int) '.', 2),
                Map.entry((int) 'c', 6)
        ));
        result.put("currency", currencyGlyphs);
        result.put("currency_creative", currencyGlyphs);
        result.put("smallnums", currencyGlyphs);
        result.put("addons_tab_line1", currencyGlyphs);
        result.put("addons_tab_line2", currencyGlyphs);

        var smallGlyphs = new Int2IntArrayMap();
        smallGlyphs.putAll(Map.<Integer, Integer>ofEntries(
                Map.entry((int) 'a', 6), Map.entry((int) 'b', 6),
                Map.entry((int) 'c', 6), Map.entry((int) 'd', 6),
                Map.entry((int) 'e', 6), Map.entry((int) 'f', 6),
                Map.entry((int) 'g', 6), Map.entry((int) 'h', 6),
                Map.entry((int) 'i', 4), Map.entry((int) 'j', 6),
                Map.entry((int) 'k', 6), Map.entry((int) 'l', 6),
                Map.entry((int) 'm', 6), Map.entry((int) 'n', 6),
                Map.entry((int) 'o', 6), Map.entry((int) 'p', 6),
                Map.entry((int) 'q', 6), Map.entry((int) 'r', 6),
                Map.entry((int) 's', 6), Map.entry((int) 't', 6),
                Map.entry((int) 'u', 6), Map.entry((int) 'v', 6),
                Map.entry((int) 'w', 6), Map.entry((int) 'x', 6),
                Map.entry((int) 'y', 6), Map.entry((int) 'z', 6)
        ));
        var smallTallGlyphs = new Int2IntArrayMap();
        smallTallGlyphs.putAll(Map.<Integer, Integer>ofEntries(
                Map.entry((int) 'a', 6), Map.entry((int) 'b', 6),
                Map.entry((int) 'c', 6), Map.entry((int) 'd', 6),
                Map.entry((int) 'e', 6), Map.entry((int) 'f', 6),
                Map.entry((int) 'g', 6), Map.entry((int) 'h', 6),
                Map.entry((int) 'i', 4), Map.entry((int) 'j', 6),
                Map.entry((int) 'k', 6), Map.entry((int) 'l', 6),
                Map.entry((int) 'm', 6), Map.entry((int) 'n', 6),
                Map.entry((int) 'o', 6), Map.entry((int) 'p', 6),
                Map.entry((int) 'q', 6), Map.entry((int) 'r', 6),
                Map.entry((int) 's', 6), Map.entry((int) 't', 6),
                Map.entry((int) 'u', 6), Map.entry((int) 'v', 6),
                Map.entry((int) 'w', 6), Map.entry((int) 'x', 6),
                Map.entry((int) 'y', 6), Map.entry((int) 'z', 6),
                Map.entry((int) '1', 4), Map.entry((int) '2', 5),
                Map.entry((int) '3', 5), Map.entry((int) '4', 5),
                Map.entry((int) '5', 5), Map.entry((int) '6', 5),
                Map.entry((int) '7', 5), Map.entry((int) '8', 5),
                Map.entry((int) '9', 5), Map.entry((int) '0', 5),
                Map.entry((int) '/', 4), Map.entry((int) '-', 3),
                Map.entry((int) '.', 2)
        ));
        result.put("small_bossbar_line2", currencyGlyphs);
        result.put("small", smallGlyphs);
        result.put("bossbar_small_1", smallTallGlyphs);
        result.put("bossbar_small_2", smallTallGlyphs);

        GLYPH_WIDTHS_V2 = Map.copyOf(result);
    }

    public static final Map<String, Map<Character, Character>> fontmaps;

    static {
        var asciiFontmap = new HashMap<Character, Character>();
        for (int asciiChar : GLYPH_WIDTHS.keySet()) {
            asciiFontmap.put((char) asciiChar, (char) asciiChar);
        }

        var tempFontmaps = new HashMap<String, Map<Character, Character>>();
        tempFontmaps.put("ascii", asciiFontmap);
        try (var is = FontUtil.class.getResourceAsStream("/fonts.json")) {
            if (is != null) {
                var allFonts = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

                for (var entry : allFonts.entrySet()) {
                    var json = entry.getValue().getAsJsonObject();
                    var chars = new HashMap<Character, Character>();
                    for (var entry2 : json.entrySet()) {
                        chars.put(entry2.getKey().charAt(0), entry2.getValue().getAsString().charAt(0));
                    }
                    tempFontmaps.put(entry.getKey(), Map.copyOf(chars));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fontmaps = Map.copyOf(tempFontmaps);
    }

    public static final Int2IntArrayMap ALL_GLYPH_WIDTHS = new Int2IntArrayMap();

    // Represents -2^index pixels of space
    public static final @NotNull List<String> NEGATIVE_SPACE = List.of(
            "\uF801", // -1
            "\uF802", // -2
            "\uF804", // -4
            "\uF808", // -8
            "\uF809", // -16
            "\uF80A", // -32
            "\uF80B", // -64
            "\uF80C", // -128
            "\uF80D", // -256
            "\uF80E", // -512
            "\uF80F"  // -1024
    );

    // Represents 2^index pixels of space
    public static final @NotNull List<String> POSITIVE_SPACE = List.of(
            "\uF821", // 1
            "\uF822", // 2
            "\uF824", // 4
            "\uF828", // 8
            "\uF829", // 16
            "\uF82A", // 32
            "\uF82B", // 64
            "\uF82C", // 128
            "\uF82D", // 256
            "\uF82E", // 512
            "\uF82F"  // 1024
    );

    static {
        ALL_GLYPH_WIDTHS.putAll(GLYPH_WIDTHS);

        for (int i = 0; i < POSITIVE_SPACE.size(); i++) {
            ALL_GLYPH_WIDTHS.put(POSITIVE_SPACE.get(i).charAt(0), 1 << i);
        }

        for (int i = 0; i < NEGATIVE_SPACE.size(); i++) {
            ALL_GLYPH_WIDTHS.put(NEGATIVE_SPACE.get(i).charAt(0), -(1 << i));
        }

        ALL_GLYPH_WIDTHS.putAll(Map.ofEntries(
                Map.entry((int) 'ᴀ', 6), Map.entry((int) 'ʙ', 6),
                Map.entry((int) 'ᴄ', 6), Map.entry((int) 'ᴅ', 6),
                Map.entry((int) 'ᴇ', 6), Map.entry((int) 'ꜰ', 6),
                Map.entry((int) 'ɢ', 6), Map.entry((int) 'ʜ', 6),
                Map.entry((int) 'ɪ', 4), Map.entry((int) 'ᴊ', 6),
                Map.entry((int) 'ᴋ', 6), Map.entry((int) 'ʟ', 6),
                Map.entry((int) 'ᴍ', 6), Map.entry((int) 'ɴ', 6),
                Map.entry((int) 'ᴏ', 6), Map.entry((int) 'ᴘ', 6),
                Map.entry((int) 'ǫ', 6), Map.entry((int) 'ʀ', 6),
                Map.entry((int) 'ѕ', 6), Map.entry((int) 'ᴛ', 6),
                Map.entry((int) 'ᴜ', 6), Map.entry((int) 'ᴠ', 6),
                Map.entry((int) 'ᴡ', 6), Map.entry((int) 'х', 6),
                Map.entry((int) 'ʏ', 6), Map.entry((int) 'ᴢ', 6)
        ));

        try (var is = FontUtil.class.getResourceAsStream("/sprites.json")) {
            if (is != null) {
                var allSprites = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
                for (var entry : allSprites) {
                    var obj = entry.getAsJsonObject();
                    if (!obj.has("fontChar")) continue;
                    var width = obj.get("width");
                    ALL_GLYPH_WIDTHS.put(obj.get("fontChar").getAsInt(),
                            (width instanceof JsonPrimitive prim ? prim.getAsInt() : 0) + 1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        var allWidths = new Int2IntArrayMap();
        allWidths.putAll(GLYPH_WIDTHS);
        for (var entry : GLYPH_WIDTHS_V2.entrySet()) {
            var fontmap = Objects.requireNonNull(fontmaps.get(entry.getKey()), entry.getKey());
            for (var pair : entry.getValue().int2IntEntrySet()) {
                char remapped = fontmap.getOrDefault((char) pair.getIntKey(), Character.MAX_VALUE);
                if (remapped == Character.MAX_VALUE) {
                    continue;
//                    throw new RuntimeException("Unknown glyph: " + pair.getIntKey() + " in font: " + entry.getKey());
                }
                allWidths.put(remapped, pair.getIntValue());
            }
        }
        allWidths.putAll(Map.ofEntries(
                Map.entry((int) 'ᴀ', 6), Map.entry((int) 'ʙ', 6),
                Map.entry((int) 'ᴄ', 6), Map.entry((int) 'ᴅ', 6),
                Map.entry((int) 'ᴇ', 6), Map.entry((int) 'ꜰ', 6),
                Map.entry((int) 'ɢ', 6), Map.entry((int) 'ʜ', 6),
                Map.entry((int) 'ɪ', 4), Map.entry((int) 'ᴊ', 6),
                Map.entry((int) 'ᴋ', 6), Map.entry((int) 'ʟ', 6),
                Map.entry((int) 'ᴍ', 6), Map.entry((int) 'ɴ', 6),
                Map.entry((int) 'ᴏ', 6), Map.entry((int) 'ᴘ', 6),
                Map.entry((int) 'ǫ', 6), Map.entry((int) 'ʀ', 6),
                Map.entry((int) 'ѕ', 6), Map.entry((int) 'ᴛ', 6),
                Map.entry((int) 'ᴜ', 6), Map.entry((int) 'ᴠ', 6),
                Map.entry((int) 'ᴡ', 6), Map.entry((int) 'х', 6),
                Map.entry((int) 'ʏ', 6), Map.entry((int) 'ᴢ', 6)
        ));
        allWidths.put('\uF824', allWidths.get(' '));
        for (var sprite : BadSprite.SPRITE_MAP.values()) {
            if (sprite.fontChar() == 0 || sprite.modelOrNull() != null) continue;
            allWidths.put(sprite.fontChar(), sprite.width() + 1);
        }
        for (int i = 0; i < POSITIVE_SPACE.size(); i++)
            allWidths.put(POSITIVE_SPACE.get(i).charAt(0), 1 << i);
        for (int i = 0; i < NEGATIVE_SPACE.size(); i++)
            allWidths.put(NEGATIVE_SPACE.get(i).charAt(0), -(1 << i));
        ALL_GLYPH_WIDTHS_V2 = Int2IntMaps.unmodifiable(allWidths);
    }

    public static final int DEFAULT_HEIGHT = 9;

    public static int measureText(@NotNull String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = ALL_GLYPH_WIDTHS.getOrDefault(codePoint, -1);
            if (glyphWidth == -1) {
                throw new RuntimeException("Unknown glyph: " + codePoint + " (" + (char) codePoint + ")");
            }
            width += glyphWidth;
        }
        return width;
    }

    public static int measureTextV2(@NotNull String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            int charWidth = ALL_GLYPH_WIDTHS_V2.getOrDefault(c, Integer.MAX_VALUE);
            if (charWidth == Integer.MAX_VALUE) {
                throw new RuntimeException("unknown char: " + c + " in " + text);
            }
            width += charWidth;
        }
        return width;
    }

    public static int measureTextV2(@NotNull Component comp) {
        int width = 0;
        if (comp instanceof TextComponent text) {
            width += measureTextV2(text.content());
        } else if (comp instanceof TranslatableComponent translate) {
            throw new RuntimeException("Cannot measure unresolved translation key: " + translate.key());
        } else {
            throw new UnsupportedOperationException("cannot measure unsupported component type: " + comp.getClass().getName());
        }

        for (Component child : comp.children())
            width += measureTextV2(child);
        return width;
    }

    public static int measureText(@NotNull String font, @NotNull String text) {
        var glyphWidths = GLYPH_WIDTHS_V2.get(font);
        Check.notNull(glyphWidths, "Unknown font: " + font);

        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = glyphWidths.getOrDefault(codePoint, -1);
            if (glyphWidth == -1) {
                throw new RuntimeException("Unknown glyph: " + codePoint + " (" + (char) codePoint + ")");
            }
            width += glyphWidth;
        }
        return width;
    }

    public static @NotNull String rewrite(@NotNull String font, @NotNull String text) {
        if (font.equals("default")) return text;
        var charmap = fontmaps.get(font);
        Check.notNull(charmap, "Unknown font: " + font);

        var result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                result.append('\uF824');
                continue;
            }

            var replacement = charmap.get(c);
            if (replacement == null) {
                if (!POSITIVE_SPACE.contains(String.valueOf(c)) && !NEGATIVE_SPACE.contains(String.valueOf(c))) {
                    logger.warn("Unknown character: " + c + " in font: " + font);
                }
                replacement = c;
            }
            result.append(replacement);
        }
        return result.toString();
    }

    public static @NotNull String computeOffset(int offset) {
        if (offset == 0) return "";

        var chars = offset > 0 ? POSITIVE_SPACE : NEGATIVE_SPACE;
        var sb = new StringBuilder();
        offset = Math.abs(offset);
        Check.argCondition(offset > 0b1111111111, "Oof too big!");

        for (int i = 0; i < 10; i++) {
            if ((offset & (1 << i)) != 0) {
                sb.append(chars.get(i));
            }
        }

        return sb.toString();
    }

    public static @NotNull TextColor computeVerticalOffset(int offset) {
        if (offset < -50 || offset > 205)
            throw new IllegalArgumentException("Offset out of range: " + offset);

        return TextColor.color(0x4E5A00 | ((offset + 50) & 0xFF));
    }

    public static @NotNull ShadowColor computeVerticalOffsetShadow(int offset) {
        return ShadowColor.shadowColor(computeVerticalOffset(offset), 80);
    }

    // This enum must match exactly the values in the text shader.
    // May only have 8 values.
    public enum Size {
        S1X1,
        S2X1,
        S3X1,
        S3X2,
        S3X3,
        S4X3,
        UNUSED1,
        UNUSED2;

        public static Size fromSize(int width, int height) {
            if (width == 1 && height == 1) return S1X1;
            if (width == 2 && height == 1) return S2X1;
            if (width == 3 && height == 1) return S3X1;
            if (width == 3 && height == 2) return S3X2;
            if (width == 3 && height == 3) return S3X3;
            if (width == 4 && height == 3) return S4X3;
            throw new IllegalArgumentException("Invalid size: " + width + "x" + height);
        }
    }

    public static @NotNull TextColor computeShadowPos(@NotNull Size size, int slotX, int slotY) {
        int slotIndex = slotX + slotY * 9;
        return TextColor.color(78, (11 << 2) | (size.ordinal() >> 1),
                ((size.ordinal() & 1) << 7) | (slotIndex & 0x7F));
    }

    /**
     * Strips all "invalid" characters for use by players. The returned string may be empty, but will never be null.
     *
     * <p>Currently this is a subset of ASCII but we may allow others in the future.</p>
     *
     * <p>If this is changed, the Go version in `common-go` must also be changed.</p>
     *
     * @param input The string to sanitize
     * @return The sanitized string
     */
    public static @NotNull String stripInvalidChars(@NotNull String input) {
        return input.codePoints().filter(i -> i >= 0x20 && i <= 0x7E)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static @NotNull String stripNonAlphanumeric(@NotNull String input) {
        return input.codePoints().filter(i -> (i >= 0x30 && i <= 0x39) || (i >= 0x41 && i <= 0x5A) || (i >= 0x61 && i <= 0x7A))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString().toLowerCase(Locale.ROOT);
    }

}
