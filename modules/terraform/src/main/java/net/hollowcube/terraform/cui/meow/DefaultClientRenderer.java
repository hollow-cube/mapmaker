package net.hollowcube.terraform.cui.meow;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.cui.meow.displays.AabbDisplay;
import net.hollowcube.terraform.cui.meow.displays.DefaultClientRenderDisplay;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
        var currentDisplay = displays.get(current);
        if (currentDisplay instanceof AabbDisplay aabbDisplay) {
            aabbDisplay.rescale(point1, point2);
            return;
        } else if (currentDisplay != null) {
            currentDisplay.remove();
        }

        displays.put(current, new AabbDisplay(player, point1, point2, this.context.getColors(), type));
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
            line(points.get(i), points.get(i + 1));
        }
    }

    @Override
    public void line(@NotNull Point p1, @NotNull Point p2) {
        // drawLine(p2, p1, NamedTextColor.GOLD);
    }

    @Override
    public void clearAll() {
        for (var entry : this.displays.values()) {
            entry.remove();
        }
        this.displays.clear();
    }

    @Override
    public void remove(String id) {
        var remove = this.displays.remove(id);
        if (remove != null) {
            remove.remove();
        }
    }

    @Override
    public void switchTo(@NotNull RenderContext context, boolean store) {
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
}
