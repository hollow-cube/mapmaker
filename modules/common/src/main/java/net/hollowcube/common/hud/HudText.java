package net.hollowcube.common.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final int FEATURE_OFFSET = 0xB0;

    private HudText() {
    }

    public static @NotNull Component text(@NotNull String text, @NotNull HudAnchor anchor, int yOffset, @NotNull TextColor tint) {
        return Component.text(text).color(KILL).shadowColor(marker(anchor, yOffset, tint));
    }

    /**
     * Anchors an existing component tree: every run keeps its layout and non-color style
     * (hover events, etc), but its color moves into the marker tint (quantized to RGB 3:3:2)
     * with the visible color replaced by {@link #KILL}. Uncolored runs tint white.
     */
    public static @NotNull Component anchored(@NotNull Component component, @NotNull HudAnchor anchor, int yOffset) {
        return anchored(component, anchor, yOffset, NamedTextColor.WHITE);
    }

    private static @NotNull Component anchored(@NotNull Component component, @NotNull HudAnchor anchor, int yOffset, @NotNull TextColor inherited) {
        var tint = component.color() != null ? component.color() : inherited;
        var result = component.color(KILL).shadowColor(marker(anchor, yOffset, tint));
        if (!component.children().isEmpty()) {
            result = result.children(component.children().stream()
                    .map(child -> anchored(child, anchor, yOffset, tint))
                    .toList());
        }
        return result;
    }

    public static @NotNull ShadowColor marker(@NotNull HudAnchor anchor, int yOffset, @NotNull TextColor tint) {
        if (yOffset < -128 || yOffset > 127) throw new IllegalArgumentException("yOffset out of range: " + yOffset);

        return ShadowColor.shadowColor((SENTINEL_ALPHA << 24)
                | ((FEATURE_ANCHOR | anchor.ordinal()) << 16)
                | ((yOffset + 128) << 8)
                | rgb332(tint));
    }

    /**
     * A relative vertical offset marker: moves the glyph down by yOffset from wherever it was
     * drawn, without anchoring. For text positioned within a vanilla-placed surface (container
     * titles, toasts). 12-bit offset: low nibble of R + all of G.
     */
    public static @NotNull ShadowColor offset(int yOffset, @NotNull TextColor tint) {
        if (yOffset < -2048 || yOffset > 2047) throw new IllegalArgumentException("yOffset out of range: " + yOffset);

        int biased = yOffset + 2048;
        return ShadowColor.shadowColor((SENTINEL_ALPHA << 24)
                | ((FEATURE_OFFSET | (biased >> 8)) << 16)
                | ((biased & 0xFF) << 8)
                | rgb332(tint));
    }

    private static int rgb332(@NotNull TextColor tint) {
        return (Math.round(tint.red() * 7 / 255f) << 5)
                | (Math.round(tint.green() * 7 / 255f) << 2)
                | Math.round(tint.blue() * 3 / 255f);
    }
}
