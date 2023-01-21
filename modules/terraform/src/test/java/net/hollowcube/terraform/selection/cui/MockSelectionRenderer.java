package net.hollowcube.terraform.selection.cui;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockSelectionRenderer implements SelectionRenderer {
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

    private final AtomicBoolean rendering = new AtomicBoolean(false);
    @Override
    public void begin() {
        assertFalse(rendering.getAndSet(true), "Already rendering");
    }
    @Override
    public void end() {
        assertTrue(rendering.getAndSet(false), "Not rendering");
    }
    public void assertNotRendering() {
        assertFalse(rendering.get());
    }

    // Drawn shapes
    //todo not sure its worth testing this

    @Override
    public void cuboid(@NotNull Point point1, @NotNull Point point2) {

    }

    @Override
    public void point(@NotNull Point point) {

    }

    @Override
    public void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4) {

    }
}
