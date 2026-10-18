package net.hollowcube.mapmaker.map.util.debug;

import net.minestom.server.coordinate.Point;

/// A line or box drawn in world by [DebugDraw].
public sealed interface DebugShape {
    /// The client renders a display out to `viewRange * 64` blocks, scaled by its entity distance (50% to 500%) and
    /// its render distance (1x to 2.5x). Debug shapes default to well past anything a client would load.
    float DEFAULT_VIEW_RANGE = 16;

    /// @param dashed flow dashes from start to end instead of drawing it solid
    /// @param seeThrough   also draw it through walls
    /// @param viewRange see [#DEFAULT_VIEW_RANGE]
    record Line(Point start, Point end, WireColor color, boolean dashed, boolean seeThrough, float viewRange) implements DebugShape {
        public Line(Point start, Point end, WireColor color) {
            this(start, end, color, false, false);
        }

        public Line(Point start, Point end, WireColor color, boolean dashed, boolean seeThrough) {
            this(start, end, color, dashed, seeThrough, DEFAULT_VIEW_RANGE);
        }
    }

    /// Sizes are sent in sixteenths when every side fits (up to 255/16 blocks), otherwise whole blocks (up to 255), and
    /// rounded to that unit, so eg a 0.6 wide box draws 0.625 wide. Boxes past 255 blocks are drawn as twelve exact lines.
    ///
    /// @param dashed flow dashes along each edge instead of drawing it solid
    /// @param seeThrough   also draw it through walls
    /// @param viewRange see [#DEFAULT_VIEW_RANGE]
    record Box(Point min, Point max, WireColor color, boolean dashed, boolean seeThrough, float viewRange) implements DebugShape {
        public Box(Point min, Point max, WireColor color) {
            this(min, max, color, false, false);
        }

        public Box(Point min, Point max, WireColor color, boolean dashed, boolean seeThrough) {
            this(min, max, color, dashed, seeThrough, DEFAULT_VIEW_RANGE);
        }
    }
}
