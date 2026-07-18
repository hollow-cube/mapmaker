package net.hollowcube.common.hud;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;

final class HudBaker {
    private static final TextColor DEFAULT_TINT = NamedTextColor.WHITE;
    private static final TextColor DEFAULT_SHADOW_TINT = shadowTint(DEFAULT_TINT);

    private final TextComponent.Builder out = Component.text();
    private int cursor = 0;

    private HudBaker() {
    }

    /// Returns a single text component representing the given anchored hud nodes,
    /// always with a net-0 advance (ie the results can be appended safely)
    static Component bake(List<HudNode.Anchored> roots) {
        var baker = new HudBaker();
        for (var root : roots) baker.walk(root.node(), root.anchor(), 0, 0, false);
        baker.moveTo(0); // always net-0 advance
        return baker.out.build();
    }

    /// `shadow` marks the displaced darkened pass of a [HudNode.Shadow] subtree.
    private void walk(HudNode node, HudAnchor anchor, int x, int y, boolean shadow) {
        switch (node) {
            case HudNode.Text(var content) -> {
                moveTo(x);
                out.append(shadow
                    ? HudText.anchored(content, anchor, y, HudBaker::shadowTint)
                    : HudText.anchored(content, anchor, y));
                cursor += FontUtil.measureText(content);
            }
            case HudNode.Sprite(var sprite) -> {
                moveTo(x);
                out.append(Component.text(FontUtil.computeOffset(sprite.offsetX()) + sprite.fontChar(), HudText.COLOR_MARKER)
                    .shadowColor(HudText.buildAnchorShadowMarker(anchor, y, spriteTint(shadow))));
                cursor += sprite.offsetX() + sprite.width() + 1 - sprite.rightOffset();
            }
            case HudNode.Raw(var component, var width) -> {
                if (shadow) break; // pre-marked content cannot be re-tinted or displaced
                moveTo(x);
                out.append(component);
                cursor += width;
            }
            case HudNode.HBackground(var background, var padding, var child) -> {
                int contentWidth = width(child) + 2 * padding;
                moveTo(x);
                out.append(Component.text(background.build(contentWidth), HudText.COLOR_MARKER)
                    .shadowColor(HudText.buildAnchorShadowMarker(anchor, y, spriteTint(shadow))));
                cursor += background.advance(contentWidth);
                walk(child, anchor, x + padding, y, shadow);
            }
            case HudNode.Shadow(var child) -> {
                // The darkened copy first so the real pass paints over it.
                if (!shadow) walk(child, anchor, x + 1, y + 1, true);
                walk(child, anchor, x, y, shadow);
            }
            case HudNode.Offset(var dx, var dy, var child) -> walk(child, anchor, x + dx, y + dy, shadow);
            case HudNode.Frame(var width, var align, var child) ->
                walk(child, anchor, x + alignOffset(align, width - width(child)), y, shadow);
            case HudNode.Stack(var axis, var gap, var align, var children) -> {
                switch (axis) {
                    case X -> {
                        int cx = x;
                        for (var child : children) {
                            walk(child, anchor, cx, y, shadow);
                            cx += width(child) + gap;
                        }
                    }
                    case Y -> {
                        int width = width(node);
                        int cy = y;
                        for (var child : children) {
                            walk(child, anchor, x + alignOffset(align, width - width(child)), cy, shadow);
                            cy += height(child) + gap;
                        }
                    }
                    case Z -> {
                        int width = width(node);
                        for (var child : children) {
                            walk(child, anchor, x + alignOffset(align, width - width(child)), y, shadow);
                        }
                    }
                }
            }
        }
    }

    private static TextColor shadowTint(TextColor tint) {
        return TextColor.color(tint.red() / 4, tint.green() / 4, tint.blue() / 4);
    }

    private static TextColor spriteTint(boolean shadow) {
        return shadow ? DEFAULT_SHADOW_TINT : DEFAULT_TINT;
    }

    private void moveTo(int x) {
        if (x != cursor) out.append(Component.text(FontUtil.computeOffset(x - cursor)));
        cursor = x;
    }

    private static int alignOffset(HudNode.Align align, int slack) {
        return switch (align) {
            case LEFT -> 0;
            case CENTER -> slack / 2;
            case RIGHT -> slack;
        };
    }

    static int width(HudNode node) {
        return switch (node) {
            case HudNode.Text(var content) -> FontUtil.measureText(content);
            case HudNode.Sprite(var sprite) -> sprite.width();
            case HudNode.Raw(var _, var width) -> width;
            case HudNode.HBackground(var _, var padding, var child) -> width(child) + 2 * padding;
            case HudNode.Shadow(var child) -> width(child);
            case HudNode.Offset(var _, var _, var child) -> width(child);
            case HudNode.Frame(var width, var _, var _) -> width;
            case HudNode.Stack(var axis, var gap, var _, var children) -> {
                int width = 0;
                if (axis == HudNode.Axis.X) {
                    for (var child : children) width += width(child) + gap;
                    yield children.isEmpty() ? 0 : width - gap;
                }
                for (var child : children) width = Math.max(width, width(child));
                yield width;
            }
        };
    }

    static int height(HudNode node) {
        return switch (node) {
            case HudNode.Text _ -> FontUtil.DEFAULT_HEIGHT;
            case HudNode.Sprite _ -> 0; // sprite data has no height
            case HudNode.Raw _ -> 0;
            case HudNode.HBackground(var _, var _, var child) -> height(child);
            case HudNode.Shadow(var child) -> height(child);
            case HudNode.Offset(var _, var _, var child) -> height(child);
            case HudNode.Frame(var _, var _, var child) -> height(child);
            case HudNode.Stack(var axis, var gap, var _, var children) -> {
                int height = 0;
                if (axis == HudNode.Axis.Y) {
                    for (var child : children) height += height(child) + gap;
                    yield children.isEmpty() ? 0 : height - gap;
                }
                for (var child : children) height = Math.max(height, height(child));
                yield height;
            }
        };
    }
}
