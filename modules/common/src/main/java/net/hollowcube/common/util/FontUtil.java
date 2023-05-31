package net.hollowcube.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FontUtil {
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
            Map.entry(8226, 3), Map.entry(42, 5), Map.entry(35, 6), Map.entry(47, 6),
            Map.entry(92, 6), Map.entry(40, 5), Map.entry(41, 5), Map.entry(123, 5),
            Map.entry(125, 5), Map.entry(91, 4), Map.entry(93, 4), Map.entry(45, 6),
            Map.entry(173, 3), Map.entry(8212, 6), Map.entry(95, 6), Map.entry(171, 6),
            Map.entry(187, 6), Map.entry(34, 5), Map.entry(39, 3), Map.entry(32, 4),
            Map.entry(162, 5), Map.entry(164, 4), Map.entry(36, 6), Map.entry(402, 6),
            Map.entry(163, 6), Map.entry(43, 6), Map.entry(247, 6), Map.entry(61, 6),
            Map.entry(62, 5), Map.entry(60, 5), Map.entry(177, 6), Map.entry(8776, 8),
            Map.entry(126, 7), Map.entry(172, 6), Map.entry(8734, 8), Map.entry(8721, 6),
            Map.entry(8730, 9), Map.entry(37, 6), Map.entry(9786, 9), Map.entry(9787, 9),
            Map.entry(9788, 9), Map.entry(9792, 6), Map.entry(9794, 8), Map.entry(9824, 8),
            Map.entry(9827, 8), Map.entry(9829, 8), Map.entry(9830, 8), Map.entry(64, 7),
            Map.entry(38, 6), Map.entry(182, 7), Map.entry(167, 6), Map.entry(174, 7),
            Map.entry(124, 2), Map.entry(94, 6), Map.entry(8962, 6), Map.entry(184, 4),
            Map.entry(8230, 5)
    );

    public static final Map<String, Map<Character, Character>> fontmaps;

    static {
        //todo rewrite this to be more sane + dynamic (read a single file with all of the fontmaps)
        try (var is = FontUtil.class.getResourceAsStream("/font_map_title.json")) {
            Check.notNull(is, "Missing map title font map");
            var json = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            var chars = new HashMap<Character, Character>();
            for (var entry : json.entrySet()) {
                chars.put(entry.getKey().charAt(0), entry.getValue().getAsString().charAt(0));
                System.out.println(entry.getKey().charAt(0) + " -> " + entry.getValue().getAsString().charAt(0));
            }
            fontmaps = Map.of("map_title", Map.copyOf(chars));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int measureText(@NotNull String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int glyphWidth = GLYPH_WIDTHS.getOrDefault(codePoint, -1);
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
            Check.notNull(replacement, "Unknown character: " + c + " in font: " + font);
            result.append(replacement);
        }
        return result.toString();
    }

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

}
