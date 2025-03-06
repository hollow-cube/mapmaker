package net.hollowcube.common.util;

import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.color.Color;
import org.jetbrains.annotations.Nullable;

public class ColorUtil {

    public static @Nullable Color fromHex(String hex) {
        return OpUtils.map(TextColor.fromCSSHexString(hex), Color::new);
    }

    public static String toHex(Color color) {
        return String.format("#%06x", color.asRGB());
    }
}
