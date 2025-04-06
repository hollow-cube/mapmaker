package net.hollowcube.common.util;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import org.jetbrains.annotations.Nullable;

public class ColorUtil {

    public static @Nullable RGBLike fromHex(String hex) {
        return OpUtils.map(TextColor.fromCSSHexString(hex), Color::new);
    }

    public static String toHex(RGBLike color) {
        return String.format("#%02x%02x%02x", color.red(), color.green(), color.blue());
    }
}
