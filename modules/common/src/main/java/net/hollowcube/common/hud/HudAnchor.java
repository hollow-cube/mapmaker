package net.hollowcube.common.hud;

/// Anchor points resolved by text shader.
/// Indices must remain in sync.
///
/// column = ord % 3, row = ord / 3
public enum HudAnchor {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, CENTER, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
}
