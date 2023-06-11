package net.hollowcube.terraform.selection.cui;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SelectionRenderer {

    enum Feature {
        CUBE,
        POINT,
        BEZIER,
    }

    boolean hasFeature(@NotNull Feature feature);

    void begin();

    void end();

    /**
     * Renders a cuboid region between the given points.
     * <p>
     * Requires {@link Feature#CUBE} support.
     */
    void cuboid(@NotNull Point point1, @NotNull Point point2);

    default void point(@NotNull Point point) {
        point(point, 0.05);
    }

    /**
     * Renders a single point at the given {@link Point}.
     * <p>
     * Requires {@link Feature#POINT} support.
     */
    void point(@NotNull Point point, double radius);

//    /**
//     * Renders a chain of bezier curves between the given points.
//     * <p>
//     * Requires {@link Feature#POINT} support.
//     */
//    void bezierChain(@NotNull List<Point> points);

    void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4);

    void lineChain(@NotNull List<Point> points);

    void line(@NotNull Point p1, @NotNull Point p2);

}
