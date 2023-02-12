package net.hollowcube.canvas.internal.standalone.sprite;

import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FontUtil {
    private FontUtil() {}

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
        Check.argCondition(offset > 0b1111111111, "Offset too big!");

        for (int i = 0; i < 10; i++) {
            if ((offset & (1 << i)) != 0) {
                sb.append(chars.get(i));
            }
        }

        return sb.toString();
    }
}
