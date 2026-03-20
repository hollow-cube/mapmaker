package net.hollowcube.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.chars.Char2CharFunction;
import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharMaps;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.hollowcube.common.util.font.FontSpacing;
import net.hollowcube.common.util.font.FontWidthMap;
import net.hollowcube.common.util.font.FontWidths;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class FontUtil {
    private FontUtil() {
    }

    private static final FontWidthMap DEFAULT_FONT_WIDTHS = FontWidthMap.loadFromResources("/char_widths.bin");

    private static final Map<String, Int2IntFunction> CUSTOM_FONT_WIDTHS;

    static {
        var result = new HashMap<String, Int2IntFunction>();

        result.put("ascii", DEFAULT_FONT_WIDTHS);
        result.put("line_0", DEFAULT_FONT_WIDTHS);
        result.put("line_1", DEFAULT_FONT_WIDTHS);
        result.put("line_2", DEFAULT_FONT_WIDTHS);
        result.put("line_3", DEFAULT_FONT_WIDTHS);
        result.put("line_3_1", DEFAULT_FONT_WIDTHS);
        result.put("line_4", DEFAULT_FONT_WIDTHS);
        result.put("line_4_1", DEFAULT_FONT_WIDTHS);
        result.put("bossbar_ascii_1", DEFAULT_FONT_WIDTHS);
        result.put("currency", FontWidths.CURRENCY_WIDTHS);
        result.put("currency_creative", FontWidths.CURRENCY_WIDTHS);
        result.put("smallnums", FontWidths.CURRENCY_WIDTHS);
        result.put("addons_tab_line1", FontWidths.CURRENCY_WIDTHS);
        result.put("addons_tab_line2", FontWidths.CURRENCY_WIDTHS);
        result.put("small_bossbar_line2", FontWidths.CURRENCY_WIDTHS);
        result.put("small", FontWidths.SMALL_WIDTHS);
        result.put("bossbar_small_1", FontWidths.SMALL_TALL_WIDTHS);
        result.put("bossbar_small_2", FontWidths.SMALL_TALL_WIDTHS);

        CUSTOM_FONT_WIDTHS = Map.copyOf(result);
    }

    public static final Map<String, Char2CharFunction> CUSTOM_FONTS;

    static {
        var tempFontmaps = new HashMap<String, Char2CharFunction>();
        tempFontmaps.put("ascii", Char2CharFunction.identity());
        try (var is = FontUtil.class.getResourceAsStream("/fonts.json")) {
            if (is != null) {
                var allFonts = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

                for (var entry : allFonts.entrySet()) {
                    var json = entry.getValue().getAsJsonObject();
                    var chars = new Char2CharOpenHashMap();
                    for (var entry2 : json.entrySet()) {
                        chars.put(entry2.getKey().charAt(0), entry2.getValue().getAsString().charAt(0));
                    }
                    tempFontmaps.put(entry.getKey(), Char2CharMaps.unmodifiable(chars));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CUSTOM_FONTS = Map.copyOf(tempFontmaps);
    }

    private static final Int2IntFunction FONT_WIDTH_PROVIDER;

    static {
        var customWidthLookup = new Int2IntArrayMap();
        for (var font : CUSTOM_FONTS.entrySet()) {
            var provider = CUSTOM_FONT_WIDTHS.get(font.getKey());
            if (provider == null) continue;
            if (!(font.getValue() instanceof Char2CharMap replacements)) continue;

            Char2CharMaps.fastForEach(replacements, entry -> {
                char from = entry.getCharKey();
                char to = entry.getCharValue();

                int width = provider.getOrDefault(from, -1);
                if (width == -1) return;

                customWidthLookup.put(to, width);
            });
        }
        customWidthLookup.putAll(FontWidths.SMALL_CAPS_WIDTHS);
        customWidthLookup.put('\uF824', customWidthLookup.get(' '));
        for (var sprite : BadSprite.SPRITE_MAP.values()) {
            if (sprite.fontChar() == 0 || sprite.modelOrNull() != null) continue;
            customWidthLookup.put(sprite.fontChar(), sprite.width() + 1);
        }
        for (int i = 0; i < FontSpacing.POSITIVE_SPACE.size(); i++)
            customWidthLookup.put(FontSpacing.POSITIVE_SPACE.get(i).charAt(0), 1 << i);
        for (int i = 0; i < FontSpacing.NEGATIVE_SPACE.size(); i++)
            customWidthLookup.put(FontSpacing.NEGATIVE_SPACE.get(i).charAt(0), -(1 << i));

        FONT_WIDTH_PROVIDER = codepoint -> {
            int width = customWidthLookup.getOrDefault(codepoint, -1);
            return width == -1 ? DEFAULT_FONT_WIDTHS.getOrDefault(codepoint, -1) : width;
        };
    }

    public static final int DEFAULT_HEIGHT = 9;

    public static int measureText(@NotNull String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = FONT_WIDTH_PROVIDER.get(codePoint);
            if (glyphWidth == -1) {
                throw new RuntimeException("Unknown glyph: " + codePoint + " (" + (char) codePoint + ")");
            }
            width += glyphWidth;
        }
        return width;
    }

    public static int measureText(@NotNull Component comp) {
        int width = 0;
        if (comp instanceof TextComponent text) {
            width += FontUtil.measureText(text.content());
        } else if (comp instanceof TranslatableComponent translatable) {
            throw new RuntimeException("Cannot measure unresolved translation key: " + translatable.key());
        } else {
            throw new UnsupportedOperationException("unsupported component type: " + comp.getClass().getName());
        }

        for (Component child : comp.children()) {
            width += measureText(child);
        }
        return width;
    }

    public static int measureText(@NotNull String font, @NotNull String text) {
        var provider = Objects.requireNonNull(
            CUSTOM_FONT_WIDTHS.get(font),
            "Unknown font: " + font
        );

        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = provider.getOrDefault(codePoint, -1);
            if (glyphWidth == -1) {
                throw new RuntimeException("Unknown glyph: " + codePoint + " (" + (char) codePoint + ")");
            }
            width += glyphWidth;
        }
        return width;
    }

    public static @NotNull Component rewrite(@NotNull String font, @NotNull Component comp) {
        if (font.equals("default")) return comp;
        if (!CUSTOM_FONTS.containsKey(font)) throw new IllegalArgumentException("Unknown font: " + font);

        if (comp instanceof TextComponent text) {
            var content = rewrite(font, text.content());
            var children = text.children().stream().map(it -> rewrite(font, it)).toList();
            return text.content(content).children(children);
        } else {
            throw new UnsupportedOperationException("unsupported component type: " + comp.getClass().getName());
        }
    }

    public static @NotNull String rewrite(@NotNull String font, @NotNull String text) {
        if (font.equals("default")) return text;
        var replacer = Objects.requireNonNull(
            CUSTOM_FONTS.get(font),
            "Unknown font: " + font
        );

        var result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(c == ' ' ? '\uF824' : replacer.getOrDefault(c, c));
        }
        return result.toString();
    }

    public static @NotNull String computeOffset(int offset) {
        return FontSpacing.compute(offset);
    }

    public static @NotNull TextColor computeVerticalOffset(int offset) {
        if (offset < -50 || offset > 205) throw new IllegalArgumentException("Offset out of range: " + offset);

        return TextColor.color(0x4E5A00 | ((offset + 50) & 0xFF));
    }

    public static @NotNull ShadowColor computeVerticalOffsetShadow(int offset) {
        return ShadowColor.shadowColor(computeVerticalOffset(offset), 80);
    }

    public static String shorten(String text, int maxWidth, int defaultCharWidth) {
        if (measureText(text) <= maxWidth) return text;

        String ellipsis = "...";
        int ellipsisWidth = measureText(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = FONT_WIDTH_PROVIDER.get(codePoint);
            if (glyphWidth == -1) glyphWidth = defaultCharWidth;
            if (glyphWidth == -1) throw new RuntimeException("Unknown glyph: " + codePoint + " (" + (char) codePoint + ")");
            if (availableWidth - glyphWidth < 0) {
                break;
            }
            result.append((char) codePoint);
            availableWidth -= glyphWidth;
        }
        result.append(ellipsis);
        return result.toString();
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
