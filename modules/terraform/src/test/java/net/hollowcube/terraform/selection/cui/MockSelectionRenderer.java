package net.hollowcube.terraform.selection.cui;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MockSelectionRenderer implements ClientRenderer {
    private final EnumSet<Feature> features;

    public MockSelectionRenderer() {
        this.features = EnumSet.allOf(Feature.class);
    }

    public MockSelectionRenderer(@NotNull EnumSet<Feature> features) {
        this.features = features;
    }

    @Override
    public boolean hasFeature(@NotNull Feature feature) {
        return true;
    }

    // Rendering state

    private final Map<String, Boolean> rendering = new ConcurrentHashMap<>();

    @Override
    public void begin(@NotNull String id) {
        assertNull(rendering.put(id, true), "Already rendering " + id);
    }

    @Override
    public void end(@NotNull String id) {
        assertEquals(true, rendering.remove(id), "Not rendering " + id);
    }

    // Drawn shapes
    //todo not sure its worth testing this

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2, @NotNull ClientRenderer.RenderType type) {

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
    public void line(@NotNull Point p1, @NotNull Point p2, RenderType primary) {

    }

    @Override
    public void clearAll() {

    }

    @Override
    public void remove(String id) {

    }

    @Override
    public void switchTo(@NotNull RenderContext context, boolean store) {

    }

    @Override
    public @NotNull RenderContext getContext() {
        return RenderContext.NORMAL;
    }

}
