package net.hollowcube.terraform.cui;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DebugRendererClientRenderer implements ClientRenderer {
    private final Player player;
    private final ColorScheme colorScheme;

    private final String namespace;
    //    private DebugMessage.Builder builder;
    private int id = 0;

    public DebugRendererClientRenderer(@NotNull Player player, @NotNull ColorScheme colorScheme, @NotNull String name) {
        this.player = player;
        this.colorScheme = colorScheme;

        this.namespace = String.format("sel_%s", name);
    }

    @Override
    public boolean hasFeature(@NotNull Feature feature) {
        return true;
    }

    @Override
    public void begin(@NotNull String id2) {
//        builder = DebugMessage.builder()
//                .clear(namespace);
        id = 0;
    }

    @Override
    public void end(@NotNull String id2) {
//        builder.build().sendTo(player);
//        builder = null;
    }

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2) {
//        builder.set(nextId(), Shape.box()
//                .start(CoordinateUtil.min(point1, point2))
//                .end(CoordinateUtil.max(point1, point2))
//                .edgeColor(colorScheme.primary())
//                .edgeLayer(Layer.TOP)
//                .faceColor(0x0)
////                .faceColor(colorScheme.secondary())
//                .build());
    }

    @Override
    public void point(@NotNull Point point, double radius) {
//        builder.set(nextId(), Shape.box()
//                .start(point.sub(radius))
//                .end(point.add(radius))
//                .edgeColor(colorScheme.primary())
//                .faceColor(colorScheme.secondary())
//                .build());
    }

    @Override
    public void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4) {
//        builder.set(nextId(), Shape.spline()
//                .point(p1).point(p2)
//                .point(p3).point(p4)
//                .type(SplineShape.Type.BEZIER)
//                .color(colorScheme.primary())
//                .layer(Layer.TOP)
//                .build());
    }

    @Override
    public void line(@NotNull Point p1, @NotNull Point p2) {
//        builder.set(nextId(), Shape.line()
//                .point(p1)
//                .point(p2)
//                .type(LineShape.Type.SINGLE)
//                .color(colorScheme.primary())
//                .layer(Layer.TOP)
//                .build());
    }

    @Override
    public void lineChain(@NotNull List<Point> points) {
//        var b = Shape.line();
//        for (var point : points) {
//            b.point(point);
//        }
//        builder.set(nextId(), b
//                .type(LineShape.Type.SINGLE)
//                .color(colorScheme.primary())
//                .layer(Layer.TOP)
//                .build());
    }

    private @NotNull String nextId() {
        return String.format("%s:%d", namespace, id++);
    }
}
