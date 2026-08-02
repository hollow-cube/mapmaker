package net.hollowcube.common.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/// HUD text utilities to construct the marker color for various behavior.
/// Main text is always [COLOR_MARKER], which the shader discards.
///
/// A=0x4E sentinel, R=0xA0|anchor, G=yOffset+128, B=RGB 3:3:2 tint
public final class HudText {
    public static final TextColor COLOR_MARKER = TextColor.color(0x4EB000);

    public static final int MAX_HOVER_EFFECT_TOOLTIP_WIDTH = 2047;

    private static final int SENTINEL_ALPHA = 0x4E;
    private static final int FEATURE_ANCHOR = 0xA0;
    private static final int FEATURE_OFFSET = 0xB0;

    private HudText() {
    }

    /// Anchors a string at the given vertical offset from the anchor point with the given color.
    ///
    /// Caller is responsible for horizontal offset.
    public static Component anchored(String text, HudAnchor anchor, int yOffset, TextColor tint) {
        return Component.text(text).color(COLOR_MARKER).shadowColor(buildAnchorShadowMarker(anchor, yOffset, tint));
    }

    /// Anchors an existing component tree at the given vertical offset from the anchor point.
    /// Colors are quantized to RGB 3:3:2.
    ///
    /// Caller is responsible for horizontal offset.
    public static Component anchored(Component component, HudAnchor anchor, int yOffset) {
        return mapTint(component, NamedTextColor.WHITE, tint -> buildAnchorShadowMarker(anchor, yOffset, tint));
    }

    /// Like [#anchored(Component, HudAnchor, int)] but with each run's tint passed through
    /// `tintTransform` first (eg darkening every color for a fake shadow pass).
    public static Component anchored(Component component, HudAnchor anchor, int yOffset, UnaryOperator<TextColor> tintTransform) {
        return mapTint(component, NamedTextColor.WHITE, tint -> buildAnchorShadowMarker(anchor, yOffset, tintTransform.apply(tint)));
    }

    /// Shifts an existing component tree by the given vertical offset.
    /// Colors are quantized to RGB 3:3:2.
    ///
    /// Caller is responsible for horizontal offset.
    public static Component buildRelativeShadowMarker(Component component, int yOffset) {
        return mapTint(component, NamedTextColor.WHITE, tint -> buildRelativeShadowMarker(yOffset, tint));
    }

    private static Component mapTint(Component component, TextColor inherited, Function<TextColor, ShadowColor> marker) {
        var tint = Objects.requireNonNullElse(component.color(), inherited);
        var result = component.color(COLOR_MARKER).shadowColor(marker.apply(tint));
        if (!component.children().isEmpty()) {
            var newChildren = new ArrayList<Component>(component.children().size());
            for (var child : component.children())
                newChildren.add(mapTint(child, tint, marker));
            result = result.children(newChildren);
        }
        return result;
    }

    public static ShadowColor buildAnchorShadowMarker(HudAnchor anchor, int yOffset, TextColor tint) {
        if (yOffset < -128 || yOffset > 127) throw new IllegalArgumentException("yOffset out of range: " + yOffset);

        return ShadowColor.shadowColor((SENTINEL_ALPHA << 24)
                | ((FEATURE_ANCHOR | anchor.ordinal()) << 16)
                | ((yOffset + 128) << 8)
                | rgb332(tint));
    }

    /// Positions a slot hover icon glyph over its button, and describes the tooltip it rode in on so
    /// the shader can hide the part of the icon behind it.
    ///
    /// Unlike the other markers this one carries no sentinel: the glyph is identified by the data
    /// pixels baked into its texture, so the entire shadow color is payload. `x`/`y` are the icon's
    /// top left corner relative to the centre of the screen; the shader knows nothing about the
    /// container. The tooltip width takes the three bits the line count does not need, since a single
    /// line of lore easily runs past 255 pixels and a short width leaves the icon drawn over it.
    public static ShadowColor buildHoverIconShadowMarker(int x, int y, int tooltipWidth, int tooltipLines) {
        if (x < -128 || x > 127) throw new IllegalArgumentException("x out of range: " + x);
        if (y < -128 || y > 127) throw new IllegalArgumentException("y out of range: " + y);
        if (tooltipWidth < 0) throw new IllegalArgumentException("tooltipWidth out of range: " + tooltipWidth);
        if (tooltipLines < 1 || tooltipLines > 31) throw new IllegalArgumentException("tooltipLines out of range: " + tooltipLines);

        int width = Math.min(tooltipWidth, MAX_HOVER_EFFECT_TOOLTIP_WIDTH);
        return ShadowColor.shadowColor(((tooltipLines | ((width >> 8) << 5)) << 24)
                | ((x + 128) << 16)
                | ((y + 128) << 8)
                | (width & 0xFF));
    }

    public static ShadowColor buildRelativeShadowMarker(int yOffset, TextColor tint) {
        if (yOffset < -2048 || yOffset > 2047) throw new IllegalArgumentException("yOffset out of range: " + yOffset);

        int biased = yOffset + 2048;
        return ShadowColor.shadowColor((SENTINEL_ALPHA << 24)
                | ((FEATURE_OFFSET | (biased >> 8)) << 16)
                | ((biased & 0xFF) << 8)
                | rgb332(tint));
    }

    private static int rgb332(TextColor tint) {
        return (Math.round(tint.red() * 7 / 255f) << 5)
                | (Math.round(tint.green() * 7 / 255f) << 2)
                | Math.round(tint.blue() * 3 / 255f);
    }
}
