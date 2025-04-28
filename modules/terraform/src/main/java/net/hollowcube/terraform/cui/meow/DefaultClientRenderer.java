package net.hollowcube.terraform.cui.meow;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DefaultClientRenderer implements ClientRenderer {

    private final @NotNull Player player;
    List<Entity> entities = new ArrayList<>();

    public DefaultClientRenderer(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public boolean hasFeature(@NotNull Feature feature) {
        return true;
    }

    private void drawLine(Point pos1, Point pos2, RGBLike color) {
        final Instance instance = player.getInstance();
        final DisplayLine displayLine = new DisplayLine(player, pos1, pos2, color);
        entities.add(displayLine);
        displayLine.setInstance(instance);
        displayLine.addViewer(player);
        displayLine.updateViewableRule();
    }

    @Override
    public void begin(@NotNull String id) {
        clearAll();
    }

    @Override
    public void end(@NotNull String id) {}

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2) {
        var tempPos1 = point1.sub(DisplayLine.THICKNESS);
        drawLine(tempPos1.withY(point2.y()).withZ(point2.z()), point2, NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withX(point2.x()).withZ(point2.z()), point2, NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withY(point2.y()).withX(point2.x()), point2, NamedTextColor.DARK_GRAY);
        drawLine(tempPos1, tempPos1.withX(point2.x()).sub(DisplayLine.THICKNESS), NamedTextColor.RED);
        drawLine(tempPos1.add(0, DisplayLine.THICKNESS, 0), tempPos1.withY(point2.y()).sub(DisplayLine.THICKNESS), NamedTextColor.GREEN);
        drawLine(tempPos1.add(0, 0, DisplayLine.THICKNESS), tempPos1.withZ(point2.z()).sub(DisplayLine.THICKNESS), NamedTextColor.BLUE);
        drawLine(tempPos1.withX(point2.x()), tempPos1.withX(point2.x()).withY(point2.y()), NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withZ(point2.z()), tempPos1.withZ(point2.z()).withY(point2.y()), NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withY(point2.y()), tempPos1.withY(point2.y()).withX(point2.x()), NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withY(point2.y()), tempPos1.withY(point2.y()).withZ(point2.z()), NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withZ(point2.z()), tempPos1.withZ(point2.z()).withX(point2.x()), NamedTextColor.DARK_GRAY);
        drawLine(tempPos1.withX(point2.x()), tempPos1.withX(point2.x()).withZ(point2.z()), NamedTextColor.DARK_GRAY);
    }

    @Override
    public void point(@NotNull Point point, double radius) {
        final Vec mul = new Vec(1, 1, 1).normalize().mul(radius);
        cuboid(point.add(mul), point.sub(mul));
    }

    @Override
    public void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4) {

    }

    @Override
    public void lineChain(@NotNull List<Point> points) {
        for (int i = 0; i < points.size(); i++) {
            if (i + 1 >= points.size()) {
                continue;
            }
            line(points.get(i), points.get(i + 1));
        }
    }

    @Override
    public void line(@NotNull Point p1, @NotNull Point p2) {
        //drawLine(p1, p2, NamedTextColor.BLACK);
    }

    @Override
    public void clearAll() {
        for (Entity entity : entities) {
            entity.remove();
        }
        entities.clear();
    }
}
