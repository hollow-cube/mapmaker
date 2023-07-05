package net.hollowcube.terraform.cui;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class NoopClientRenderer implements ClientRenderer {
    static final NoopClientRenderer INSTANCE = new NoopClientRenderer();

    @Override
    public boolean hasFeature(@NotNull Feature feature) {
        return false;
    }

    @Override
    public void begin(@NotNull String id) {

    }

    @Override
    public void end(@NotNull String id) {

    }

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2) {

    }

    @Override
    public void point(@NotNull Point point, double radius) {

    }

    @Override
    public void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4) {

    }

    @Override
    public void lineChain(@NotNull List<Point> points) {

    }

    @Override
    public void line(@NotNull Point p1, @NotNull Point p2) {

    }

}
