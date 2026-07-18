package net.hollowcube.common.hud;

/**
 * The nine screen anchor points understood by the text shader.
 *
 * <p>The ordinal is wire format: column = ordinal % 3 (left/center/right),
 * row = ordinal / 3 (top/middle/bottom). Must match text.vsh.</p>
 */
public enum HudAnchor {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, CENTER, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
}
