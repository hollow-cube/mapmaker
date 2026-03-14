package net.hollowcube.common.util;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import org.jetbrains.annotations.Nullable;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static @Nullable Color fromHex(String hex) {
        return OpUtils.map(TextColor.fromCSSHexString(hex), Color::new);
    }

    public static String toHex(RGBLike color) {
        return String.format("#%02x%02x%02x", color.red(), color.green(), color.blue());
    }

    public static Color fromRgb(int rgb){
        return new Color(rgb);
    }

    public static int toRgb(RGBLike color){
        return ((color.red() & 0xFF) << 16) | ((color.green() & 0xFF) << 8) | (color.blue() & 0xFF);
    }

    public static RGBLike fromHsv(float h, float s, float v) {
        var color = java.awt.Color.getHSBColor(h, s, v);
        return fromRgb(color.getRGB());
    }

}
