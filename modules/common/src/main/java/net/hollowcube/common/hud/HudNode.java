package net.hollowcube.common.hud;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;

import java.util.List;

public sealed interface HudNode {

    enum Align {
        LEFT, CENTER, RIGHT
    }

    record Anchored(HudAnchor anchor, HudNode node) {
    }

    // Content

    record Text(Component content) implements HudNode {
    }

    record Sprite(BadSprite sprite) implements HudNode {
    }

    /// HudBaker escape hatch for manually rendered text. See [HudText] for utils to construct the shadow markers.
    ///
    /// Width is the final advance of the text. Inaccurate values will break layout.
    record Raw(Component component, int width) implements HudNode {
    }

    // Layout

    enum Axis {
        X, Y, Z
    }

    /// Layout stack for a set of nodes along a single axis.
    /// * `gap` separates children along the axis (ignored for Z)
    /// * `align` places children horizontally on the cross axis for Y and Z (unsupported currently for X since we dont have sprite height info)
    record Stack(Axis axis, int gap, Align align, List<HudNode> children) implements HudNode {
    }

    // Modifiers

    /// Translates without affecting layout
    record Offset(int x, int y, HudNode child) implements HudNode {
    }

    /// Fixed outer width with the measured child aligned inside it. `frame(0, CENTER)` centers
    /// the child on its origin.
    record Frame(int width, Align align, HudNode child) implements HudNode {
    }

    /// A horizontally-tiling background strip ([BackgroundSpriteSet]) behind the child, sized to
    /// the child's width plus `padding` on each side (the child draws inset by `padding`). More
    /// limited than a real panel: fixed sprite height, horizontal sizing only.
    record HBackground(BackgroundSpriteSet background, int padding, HudNode child) implements HudNode {
    }

    /// Fakes a vanilla drop shadow: the subtree is first drawn displaced (+1, +1) with every
    /// tint darkened to a quarter (vanilla's shadow color), then normally on top. Doubles the
    /// glyph count of the subtree; [Raw] content is skipped in the shadow pass.
    record Shadow(HudNode child) implements HudNode {
    }

    // Factories

    static HudNode text(Component content) {
        return new Text(content);
    }

    static HudNode text(String text) {
        return new Text(Component.text(text));
    }

    static HudNode text(String font, String text) {
        return new Text(Component.text(FontUtil.rewrite(font, text)));
    }

    static HudNode sprite(String name) {
        return new Sprite(BadSprite.require(name));
    }

    static HudNode sprite(BadSprite sprite) {
        return new Sprite(sprite);
    }

    static HudNode raw(Component component) {
        return new Raw(component, 0);
    }

    static HudNode hstack(HudNode... children) {
        return new Stack(Axis.X, 0, Align.LEFT, List.of(children));
    }

    static HudNode hstack(int gap, HudNode... children) {
        return new Stack(Axis.X, gap, Align.LEFT, List.of(children));
    }

    static HudNode vstack(HudNode... children) {
        return new Stack(Axis.Y, 0, Align.LEFT, List.of(children));
    }

    static HudNode vstack(int gap, Align align, HudNode... children) {
        return new Stack(Axis.Y, gap, align, List.of(children));
    }

    static HudNode vstack(int gap, Align align, List<HudNode> children) {
        return new Stack(Axis.Y, gap, align, List.copyOf(children));
    }

    static HudNode zstack(HudNode... children) {
        return new Stack(Axis.Z, 0, Align.LEFT, List.of(children));
    }

    default HudNode offset(int x, int y) {
        return new Offset(x, y, this);
    }

    default HudNode frame(int width, Align align) {
        return new Frame(width, align, this);
    }

    default HudNode background(BackgroundSpriteSet background, int padding) {
        return new HBackground(background, padding, this);
    }

    default HudNode shadow() {
        return new Shadow(this);
    }

    default Anchored anchored(HudAnchor anchor) {
        return new Anchored(anchor, this);
    }
}
