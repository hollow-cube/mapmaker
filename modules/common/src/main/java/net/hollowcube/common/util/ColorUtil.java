package net.hollowcube.common.util;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import org.jetbrains.annotations.Nullable;

public class ColorUtil {

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
        return fromRgb(hsvToRgb(h, s, v));
    }

    // Same as java.awt.Color#HSBtoRGB, which can't be used because initializing java.awt.Color loads libawt and
    // aborts the native image (MissingJNIRegistrationError for java/awt/GraphicsEnvironment).
    public static int hsvToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 1 -> { r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 2 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); }
                case 3 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 4 -> { r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 5 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); }
            }
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
