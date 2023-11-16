package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public record LineRegion(
        @NotNull Point pos1,
        @NotNull Point pos2
) implements Region {

    @Override
    public @NotNull Point min() {
        return CoordinateUtil.min(pos1, pos2);
    }

    @Override
    public @NotNull Point max() {
        return CoordinateUtil.min(pos1, pos2);
    }

    @NotNull
    @Override
    public Iterator<@NotNull Point> iterator() {
        return new LineIterator(pos1.add(0.5), pos2.add(0.5));
    }

    private static class LineIterator implements Iterator<@NotNull Point> {
        private final Vec step;
        private int maxSteps;
        private int currentStep = 0;

        private Point current;

        public LineIterator(Point pos1, Point pos2) {
            var line = Vec.fromPoint(pos2.sub(pos1));
            this.step = line.normalize();
            this.maxSteps = (int) Math.ceil(line.length());
            this.current = pos1;
        }

        @Override
        public boolean hasNext() {
            return currentStep < maxSteps;
        }

        @Override
        public @NotNull Point next() {
            currentStep++;
            current = current.add(step);
            return current;
        }
    }
}
