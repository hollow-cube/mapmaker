package net.hollowcube.terraform.cui.meow.displays;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.cui.meow.lines.AbstractLine;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PyramidDisplay implements DefaultClientRenderDisplay {
    private final AbstractLine
            x, z, xz, zx,
            xt, zt, xzt, zxt;

    public PyramidDisplay(
            @NotNull Player player,
            @NotNull Point center,
            int height,
            @NotNull ClientRenderer.RenderColors colors,
            @NotNull ClientRenderer.RenderType type
    ) {
        var color = type.apply(colors);
        var sideLength = height - 1;
        var vec = new Vec(sideLength, 0, sideLength).mul(2);

        var top = center.add(0, height, 0);
        var min = center.sub(sideLength, 0, sideLength);

        x = drawAxis(player, min, min.add(vec.withZ(0)), colors.x());
        xz = drawAxis(player, min.add(vec.withZ(0)), min.add(vec), color);
        z = drawAxis(player, min, min.add(vec.withX(0)), colors.z());
        zx = drawAxis(player, min.add(vec.withX(0)), min.add(vec), color);

        xt = drawLine(player, min, top, color);
        xzt = drawLine(player, min.add(vec.withZ(0)), top, color);
        zt = drawLine(player, min.add(vec.withX(0)), top, color);
        zxt = drawLine(player, min.add(vec), top, color);
    }

    @Override
    public void removeDisplay() {
        executeForAll(AbstractLine::removeDisplay);
    }

    @Override
    public void hide() {
        executeForAll(AbstractLine::hide);
    }

    @Override
    public void show() {
        executeForAll(AbstractLine::show);
    }

    public void reshape(@NotNull Point center, int height) {
        var sideLength = height - 1;
        var vec = new Vec(sideLength, 0, sideLength).mul(2);

        var top = center.add(0, height, 0);
        var min = center.sub(sideLength, 0, sideLength);

        x.reshape(min, min.add(vec.withZ(0)));
        xz.reshape(min.add(vec.withZ(0)), min.add(vec));
        z.reshape(min, min.add(vec.withX(0)));
        zx.reshape(min.add(vec.withX(0)), min.add(vec));
        xt.reshape(min, top);
        xzt.reshape(min.add(vec.withZ(0)), top);
        zt.reshape(min.add(vec.withX(0)), top);
        zxt.reshape(min.add(vec), top);
    }


    public void executeForAll(@NotNull Consumer<AbstractLine> action) {
        execute(action, x, xt, xz, xzt, z, zt, zx, zxt);
    }

    private void execute(@NotNull Consumer<AbstractLine> action, @NotNull AbstractLine... lines) {
        for (var line : lines) {
            action.accept(line);
        }
    }
}
