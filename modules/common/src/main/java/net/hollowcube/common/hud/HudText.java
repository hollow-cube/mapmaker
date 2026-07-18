package net.hollowcube.common.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Anchored HUD text, rendered through the shadow quad: the anchor payload rides
 * {@link ShadowColor} (A=0x4E sentinel, R=0xA0|anchor, G=yOffset+128, B=RGB 3:3:2 tint)
 * and the main glyph carries {@link #KILL}, which the shader discards. The carrier is
 * the player's first boss bar, whose name must be composed to net-zero advance so the
 * shader's origin assumption (floor(guiWidth / 2), 3) holds.
 */
public final class HudText {
    /** Main-glyph color of anchored runs; the shader zeroes any glyph with this exact color. */
    public static final TextColor KILL = TextColor.color(0x4EB000);

    private static final int SENTINEL_ALPHA = 0x4E;
    private static final int FEATURE_ANCHOR = 0xA0;

    private HudText() {
    }

    public static @NotNull Component text(@NotNull String text, @NotNull HudAnchor anchor, int yOffset, @NotNull TextColor tint) {
        return Component.text(text).color(KILL).shadowColor(marker(anchor, yOffset, tint));
    }

    public static @NotNull ShadowColor marker(@NotNull HudAnchor anchor, int yOffset, @NotNull TextColor tint) {
        if (yOffset < -128 || yOffset > 127) throw new IllegalArgumentException("yOffset out of range: " + yOffset);

        return ShadowColor.shadowColor((SENTINEL_ALPHA << 24)
                | ((FEATURE_ANCHOR | anchor.ordinal()) << 16)
                | ((yOffset + 128) << 8)
                | rgb332(tint));
    }

    private static int rgb332(@NotNull TextColor tint) {
        return (Math.round(tint.red() * 7 / 255f) << 5)
                | (Math.round(tint.green() * 7 / 255f) << 2)
                | Math.round(tint.blue() * 3 / 255f);
    }
}
