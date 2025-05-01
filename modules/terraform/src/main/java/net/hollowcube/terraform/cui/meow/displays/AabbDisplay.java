package net.hollowcube.terraform.cui.meow.displays;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.cui.meow.lines.AbstractLine;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AabbDisplay implements DefaultClientRenderDisplay {
    private final @NotNull AbstractLine
            x, xy, xyz, xz, xzy,
            y, yx, yzx, yz,
            z, zx, zy;
    private final Player player;

    public AabbDisplay(Player player, Point point, int expand) {
        this(player, point.sub(expand, expand, expand), point.add(expand, expand, expand), ClientRenderer.RenderColors.DEFAULT, ClientRenderer.RenderType.PRIMARY);
    }

    public AabbDisplay(Player player, Point from, Point to, ClientRenderer.RenderColors colors, ClientRenderer.RenderType type) {
        var color = type.apply(colors);
        x = drawAxis(player, from, from.withX(to.x()).sub(AbstractLine.THICKNESS), colors.x());
        xy = drawAxis(player, from.withX(to.x()), to.withZ(from.z()), color);
        xyz = drawAxis(player, to.withZ(from.z()), to, color);
        xz = drawAxis(player, from.withX(to.x()), to.withY(from.y()), color);
        xzy = drawAxis(player, to.withY(from.y()), to, color);

        y = drawAxis(player, from.add(0, AbstractLine.THICKNESS, 0), from.withY(to.y()).sub(AbstractLine.THICKNESS), colors.y());
        yx = drawAxis(player, from.withY(to.y()), to.withZ(from.z()), color);
        yz = drawAxis(player, from.withY(to.y()), to.withX(from.x()), color);
        yzx = drawAxis(player, to.withX(from.x()), to, color);

        z = drawAxis(player, from.add(0, 0, AbstractLine.THICKNESS), from.withZ(to.z()).sub(AbstractLine.THICKNESS), colors.z());
        zx = drawAxis(player, from.withZ(to.z()), to.withY(from.y()), color);
        zy = drawAxis(player, from.withZ(to.z()), to.withX(from.x()), color);

        this.player = player;
    }

    public void rescale(Point from, Point to) {
        x.reshape(from, from.withX(to.x()).sub(AbstractLine.THICKNESS));
        xy.reshape(from.withX(to.x()), to.withZ(from.z()));
        xyz.reshape(to.withZ(from.z()), to);
        xz.reshape(from.withX(to.x()), to.withY(from.y()));
        xzy.reshape(to.withY(from.y()), to);

        y.reshape(from.add(0, AbstractLine.THICKNESS, 0), from.withY(to.y()).sub(AbstractLine.THICKNESS));
        yx.reshape(from.withY(to.y()), to.withZ(from.z()));
        yz.reshape(from.withY(to.y()), to.withX(from.x()));
        yzx.reshape(to.withX(from.x()), to);

        z.reshape(from.add(0, 0, AbstractLine.THICKNESS), from.withZ(to.z()).sub(AbstractLine.THICKNESS));
        zx.reshape(from.withZ(to.z()), to.withY(from.y()));
        zy.reshape(from.withZ(to.z()), to.withX(from.x()));
    }

    @Override
    public void removeDisplay() {
        executeForAll(AbstractLine::removeDisplay);
    }

    @Override
    public void hide() {
        executeForAll(line -> line.removeViewer(player));
    }

    @Override
    public void show()  {
        executeForAll(line -> line.addViewer(player));
    }

    public void executeForAll(@NotNull Consumer<AbstractLine> action) {
        execute(action, x, xy, xyz, xz, xzy, y, yx, yzx, yz, z, zx, zy);
    }

    private void execute(@NotNull Consumer<AbstractLine> action, @NotNull AbstractLine... lines) {
        for (var line : lines) {
            action.accept(line);
        }
    }
}
