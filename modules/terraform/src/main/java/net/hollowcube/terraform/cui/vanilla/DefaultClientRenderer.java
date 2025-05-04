package net.hollowcube.terraform.cui.vanilla;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.cui.vanilla.displays.AabbDisplay;
import net.hollowcube.terraform.cui.vanilla.displays.DefaultClientRenderDisplay;
import net.hollowcube.terraform.cui.vanilla.displays.PyramidDisplay;
import net.hollowcube.terraform.cui.vanilla.lines.AbstractLine;
import net.hollowcube.terraform.cui.vanilla.lines.DefaultLine;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultClientRenderer implements ClientRenderer {

    private final @NotNull Player player;
    private final ConcurrentHashMap<RenderContext, ConcurrentHashMap<String, DefaultClientRenderDisplay>> contexts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DefaultClientRenderDisplay> displays = new ConcurrentHashMap<>();
    private RenderContext context = RenderContext.NORMAL;
    private String current;

    public DefaultClientRenderer(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public boolean hasFeature(@NotNull Feature feature) {
        return true;
    }

    @Override
    public void begin(@NotNull String id) {
        current = id;
    }

    @Override
    public void end(@NotNull String id) {
        current = null;
    }

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2, @NotNull ClientRenderer.RenderType type) {
        modifyOrCreate(
                AabbDisplay.class,
                display -> display.rescale(point1, point2),
                () -> new AabbDisplay(player, point1, point2, this.context.getColors(), type)
        );

    }

    @Override
    public void point(@NotNull Point point, double radius) {
        var mul = new Vec(1, 1, 1).normalize().mul(radius);
        cuboid(point.add(mul), point.sub(mul), RenderType.PRIMARY);
    }

    @Override
    public void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4) {

    }

    @Override
    public void lineChain(@NotNull List<Point> points) {
        for (var i = 0; i < points.size(); i++) {
            if (i + 1 >= points.size()) {
                continue;
            }
            line(points.get(i), points.get(i + 1), RenderType.PRIMARY);
        }
    }

    @Override
    public void line(@NotNull Point p1, @NotNull Point p2, RenderType primary) {
        modifyOrCreate(
                AbstractLine.class,
                display -> display.reshape(p1, p2),
                () -> new DefaultLine(player, p1, p2, primary.apply(context.getColors()))
        );
    }

    @Override
    public void clearAll() {
        for (var entry : this.displays.values()) {
            entry.removeDisplay();
        }
        this.displays.clear();
    }

    @Override
    public void remove(String id) {
        var remove = this.displays.remove(id);
        if (remove != null) {
            remove.removeDisplay();
        }
    }

    @Override
    public synchronized void switchTo(@NotNull RenderContext context, boolean store) {
        if (this.context == context) {
            return;
        }

        if (store) {
            for (var value : this.displays.values()) {
                value.hide();
            }
            this.contexts.put(this.context, this.displays);
        } else {
            clearAll();
        }
        this.displays = Objects.requireNonNullElseGet(this.contexts.get(context), ConcurrentHashMap::new);
        this.context = context;
        for (var value : this.displays.values()) {
            value.show();
        }
    }

    @Override
    public @NotNull RenderContext getContext() {
        return this.context;
    }

    @Override
    public void pyramid(@NotNull Point center, int height, @NotNull RenderType renderType) {
        modifyOrCreate(
                PyramidDisplay.class,
                display -> display.reshape(center, height),
                () -> new PyramidDisplay(player, center, height, context.getColors(), renderType)
        );
    }

    private <T extends DefaultClientRenderDisplay> void modifyOrCreate(Class<T> clazz, Consumer<T> modifier, Supplier<T> constructor) {
        var currentDisplay = displays.get(current);
        var display = OpUtils.safeCast(currentDisplay, clazz);
        if (currentDisplay != null && display == null) {
            currentDisplay.removeDisplay();
        } else if (display != null) {
            modifier.accept(display);
        } else {
            displays.put(current, constructor.get());
        }
    }
}
