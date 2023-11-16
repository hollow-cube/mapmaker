package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class BezierSurfaceRegion implements Region {
    private final List<BezierSurfaceRegionSelector.Curve> curves;
    private Point min, max;

    public BezierSurfaceRegion(@NotNull List<BezierSurfaceRegionSelector.Curve> curves) {
        this.curves = curves;

        for (var curve : curves) {
            for (var point : curve.points()) {
                if (min == null) {
                    min = point;
                    max = point;
                } else {
                    min = CoordinateUtil.min(min, point);
                    max = CoordinateUtil.max(max, point);
                }
            }
        }
    }

    @Override
    public @NotNull Point min() {
        return min;
    }

    @Override
    public @NotNull Point max() {
        return max;
    }

    @NotNull
    @Override
    public Iterator<@NotNull Point> iterator() {
        if (this.curves.size() <= 1) return Collections.emptyIterator();

        int targetSize = getMaxCurveSize();
        if (targetSize == 1) targetSize = 2;

        List<BezierSurfaceRegionSelector.Curve> curves = new ArrayList<>();
        for (var curve : this.curves) {
            curves.add(curve.withSize(targetSize));
        }

        double d = Double.MIN_VALUE;
        List<BezierSurfaceRegionSelector.Curve> connectingCurves = new ArrayList<>();
        for (int i = 0; i < targetSize; i++) {
            List<Point> points = new ArrayList<>();
            for (var curve : curves) {
                points.add(curve.get(i));
            }

            var curve = new BezierSurfaceRegionSelector.Curve(points);
            d = Math.max(d, curve.length());
            connectingCurves.add(curve);
        }

        List<Point> points = new ArrayList<>();
        int count = -1;
        int n = count < 0 ? (int) (d * 5 + 1) : count;

        for (int i = 0; i <= n; i++) {
            List<Point> pointList = new ArrayList<>();
            for (var curve : connectingCurves) {
                pointList.add(curve.getSegmentPoint((double) i / n * (this.curves.size() - 1)));
            }

            var curve = new BezierSurfaceRegionSelector.Curve(pointList);
            int stepCount = (int) (curve.length() * 3 + 1);
            for (int j = 0; j < stepCount; j++) {
                var pointOnCurve = curve.getSegmentPoint((double) j / stepCount * curve.segmentSize());
                points.add(pointOnCurve);
            }
        }

        return points.iterator();
    }

    private int getMaxCurveSize() {
        int max = 0;
        for (BezierSurfaceRegionSelector.Curve curve : curves) {
            max = Math.max(max, curve.size());
        }
        return max;
    }
}
