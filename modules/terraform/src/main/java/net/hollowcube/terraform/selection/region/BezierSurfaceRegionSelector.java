package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.selection.cui.SelectionRenderer;
import net.hollowcube.terraform.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTList;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BezierSurfaceRegionSelector implements RegionSelector {
    private final Player player;
    private final SelectionRenderer renderer;

    private final List<Curve> curves = new ArrayList<>();

    public BezierSurfaceRegionSelector(@NotNull Player player, @NotNull SelectionRenderer renderer) {
        this.player = player;
        this.renderer = renderer;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        // Add a new curve with the given point as the first
        // unless there is already a curve with the last point at this current point
        if (!curves.isEmpty() && curves.get(curves.size() - 1).last().sameBlock(point))
            return false;

        curves.add(new Curve(point));

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.bezier.explain.primary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }
        return true;
    }

    @Override
    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        // If this is the first point, a secondary selection acts the same as a primary one
        if (curves.isEmpty()) return selectPrimary(point, explain);

        // Add the given point to the last curve
        // unless the last point of the last curve is the same as the given point
        var curve = curves.get(curves.size() - 1);
        if (curve.last().sameBlock(point)) return false;
        curve.addLast(point);

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.bezier.explain.secondary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }
        return true;
    }

    @Override
    public @NotNull NBTCompound toNBT() {
        var nbt = new MutableNBTCompound();
        var curvesNBT = new ArrayList<NBTList<NBTCompound>>();
        for (var curve : curves) {
            curvesNBT.add(curve.toNBT());
        }
        nbt.set("curves", new NBTList<>(NBTType.TAG_List, curvesNBT));
        return nbt.toCompound();
    }

    @Override
    public void fromNBT(@NotNull NBTCompound nbt) {
        var curvesNBT = nbt.getList("curves");
        curves.clear();
        for (var curveNBT : curvesNBT) {
            curves.add(Curve.fromNBT((NBTList<NBTCompound>) curveNBT));
        }
        updateRender();
    }

    @Override
    public void clear() {
        curves.clear();
        updateRender();
    }

    @Override
    public @Nullable Region region() {
        return new BezierSurfaceRegion(List.copyOf(curves));
    }

    @Override
    public void changeSize(int delta, boolean changeVertical, boolean changeHorizontal) {
        throw new UnsupportedOperationException();
    }


    // Exposed details used by //loft remove
    public @NotNull List<Curve> curves() {
        return List.copyOf(curves);
    }

    public boolean removeLastPoint() {
        if (curves.isEmpty()) return false;
        var curve = curves.get(curves.size() - 1);
        curve.removeLast();
        if (curve.size() == 0) curves.remove(curves.size() - 1);
        updateRender();
        return true;
    }

    public boolean removePointClosestTo(@NotNull Point point) {
        Curve closestCurve = null;
        int closestIndex = -1;
        double bestDistance = Double.MAX_VALUE;
        for (var curve : curves) {
            for (int i = 0; i < curve.size(); i++) {
                double distance = curve.get(i).distance(point);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    closestCurve = curve;
                    closestIndex = i;
                }
            }
        }

        if (closestCurve == null) return false;
        closestCurve.remove(closestIndex);
        if (closestCurve.size() == 0) curves.remove(closestCurve);
        updateRender();
        return true;
    }


    public int getMaxCurveSize() {
        int max = 0;
        for (Curve curve : curves) {
            max = Math.max(max, curve.size());
        }
        return max;
    }

    private void updateRender() {
        try {
            renderer.begin();
            if (curves.isEmpty()) return;


            // Add a point for every control point in every curve
            for (var curve : curves) {
                for (var point : curve.points()) {
                    renderer.point(point);
                }
            }

            // Render curves between each other if there are more than one, otherwise just render a single line.
            if (curves.size() > 1) {
                var curves = new ArrayList<Curve>();

                // Ensure each curve has the same number of points, creating duplicates if necessary.
                int targetSize = getMaxCurveSize();
                if (targetSize == 1) targetSize = 2;
                for (var curve : this.curves) {
                    //todo split up the list of points and the bezier segment generation into separate concepts.
                    // This should convert the list of points to a list of segments which can be rendered.
                    curves.add(curve.withSize(targetSize));
                }

                // Create a curve between each set of points
                List<Curve> connectingCurves = new ArrayList<>();
                for (int i = 0; i < targetSize; i++) {
                    List<Point> points = new ArrayList<>();
                    for (var curve : curves) {
                        points.add(curve.get(i));
                    }
                    connectingCurves.add(new Curve(points));
                }
                curves.addAll(connectingCurves);

                // Draw each curve
                for (var curve : curves) {
                    for (var segment : curve.segments) {
                        renderer.bezierCurve(segment.getStart(), segment.getControl1(), segment.getControl2(), segment.getEnd());
                    }
                }

//            LinkedList<Vector3> linkedList;
//                LinkedList<Vector3> linkedList2;
//                Object object;
//                linkedList = new ArrayList();
//
//                 this bit recreates all of the frames with the same number of points
//
//                int n = this.maxFrameSize();
//                if (n == 1) {
//                    n = 2;
//                }
//                for (Frame a : this.getFrames()) {
//                    linkedList.add(new Frame(VectorUtil.E(new ArrayList<Vector3>(a.getPoints()), n, true)));
//                }

                // this bit creates all the intermediate splines between each point set

//                List<BezierSpline> allSplines = new ArrayList();
//                for (int i = 0; i < n; ++i) {
//                    List<Vector3> splinePoints = new ArrayList();
//                    object = new LinkedList();
//                    for (Frame frame : linkedList) {
//                        splinePoints.add(frame.getPoints().get(i));
//                    }
//                    allSplines.add(this.createSpline(splinePoints));
//                }
//                Iterator<BezierSpline> iterator = allSplines.iterator();
//                while (iterator.hasNext()) {
//                    drawSpline(iterator.next());
//                }


//                int n2 = this.curves.size() * 2 - 2;
//                for (int i = 0; i <= n2; i++) {
//                    double blah = (double) i / n2 * (this.curves.size() - 1);
//                }


//                int n2 = this.getFrameCount() * 2 - 2;
//                for (int i = 0; i <= n2; ++i) {
//                    linkedList2 = new LinkedList<Vector3>();
//                    Iterator<BezierSpline> iter = allSplines.iterator();
//                    while (iter.hasNext()) {
//                        BezierSpline spline = iter.next();
//                        linkedList2.add(spline.O((double)i / (double)n2 * (double)(this.getFrameCount() - 1)));
//                    }
//                    this.drawSpline(this.createSpline(linkedList2));
//                }
            } else if (curves.size() == 1) {
                // Draw only the first curve
                for (var segment : curves.get(0).segments) {
                    renderer.bezierCurve(segment.getStart(), segment.getControl1(), segment.getControl2(), segment.getEnd());
                }
            }
        } finally {
            renderer.end();
        }
    }

    public static class Curve {
        private final List<Point> points;
        private BezierSegment[] segments;
        private double length = -1;

        public Curve(@NotNull Point firstPoint) {
            points = new ArrayList<>();
            points.add(firstPoint);
            computePoints();
        }

        public Curve(@NotNull List<Point> points) {
            this.points = new ArrayList<>(points);
            computePoints();
        }

        public @NotNull NBTList<NBTCompound> toNBT() {
            var points = new ArrayList<NBTCompound>();
            for (var point : this.points) {
                points.add(CoordinateUtil.toNBT(point));
            }
            return new NBTList<>(NBTType.TAG_Compound, points);
        }

        public static @NotNull Curve fromNBT(@NotNull NBTList<NBTCompound> nbt) {
            var points = new ArrayList<Point>();
            for (var point : nbt) {
                points.add(CoordinateUtil.fromNBT(point));
            }
            return new Curve(points);
        }

        public @NotNull Point get(int index) {
            return points.get(index);
        }

        public @NotNull Point last() {
            return points.get(points.size() - 1);
        }

        public void addLast(@NotNull Point point) {
            points.add(point);
            computePoints();
        }

        public void removeLast() {
            if (points.isEmpty()) return;
            points.remove(points.size() - 1);
            computePoints();
        }

        public void remove(int index) {
            points.remove(index);
            computePoints();
        }

        public int size() {
            return points.size();
        }

        public int segmentSize() {
            return segments.length;
        }

        public @NotNull List<Point> points() {
            return points;
        }

        public double length() {
            if (length == -1) {
                length = 0;
                for (var segment : segments) {
                    length += segment.length();
                }
            }

            return length;
        }

        public @NotNull Curve withSize(int target) {
            if (target == 0 || target == size()) return new Curve(points);

            // Add the same point {target} times
            if (size() == 1) {
                return new Curve(Collections.nCopies(target, points.get(0)));
            }

            // Compute intermediate points to add
            List<Point> newPoints = new ArrayList<>();
            for (int i = 0; i < target; i++) {
                double d = (double) segments.length * i / (target - 1);
                newPoints.add(getSegmentPoint(d));
                // linkedList.add(c.getPointOnSegment((double)c.getSegmentCount() * (double)i / (double)(targetSize - 1)));
            }

            return new Curve(newPoints);
        }

        /**
         * Returns a point at the given distance on the given segment. The segment is the integer part, the distance is the decimal part.
         */
        public @NotNull Point getSegmentPoint(double d) {
            BezierSegment segment;
            if (d >= segments.length) {
                segment = segments[segments.length - 1];
                d = 1.0;
            } else {
                segment = segments[(int) d];
                d -= (int) d;
            }

            return segment.getPointAt(d);
        }


        private void computePoints() {
            length = -1;
            segments = new BezierSegment[points.size() - 1];
            for (int i = 0; i < this.points.size() - 1; ++i) {
                this.segments[i] = new BezierSegment(this.points.get(i), this.points.get(i + 1));
            }

            //todo append segments from points
            if (segments.length == 0) {
                return;
            }
//        Double d = points.get(0).x();
//        Double d2 = points.get(0).y();
//        Double d3 = points.get(0).z();
//        for (var point : points) {
//            if (point.x() == d) continue;
//            d = null;
//            break;
//        }
//        for (var point : points) {
//            if (point.blo() == d2) continue;
//            d2 = null;
//            break;
//        }
//        Location location = this.pointsArray;
//        int n = ((Location[])location).length;
//        for (int i = 0; i < n; ++i) {
//            Location location2 = location[i];
//            if ((double)location2.getBlockZ() == d3) continue;
//            d3 = null;
//            break;
//        }
            if (this.segments.length == 1) {
                var point = new Vec(
                        (this.segments[0].getStart().x() + this.segments[0].getEnd().x()) / 2.0,
                        (this.segments[0].getStart().y() + this.segments[0].getEnd().y()) / 2.0,
                        (this.segments[0].getStart().z() + this.segments[0].getEnd().z()) / 2.0
                );
                this.segments[0].setControl1(point);
                this.segments[0].setControl2(point);
            } else {
                this.segments[0].setXVar(0.0f);
                this.segments[0].setYVar(2.0f);
                this.segments[0].setZVar(1.0f);
                this.segments[0].setSomeVec(new Vec(
                        this.points.get(0).x() + 2.0 * this.points.get(1).x(),
                        this.points.get(0).y() + 2.0 * this.points.get(1).y(),
                        this.points.get(0).z() + 2.0 * this.points.get(1).z()
                ));
                int n2 = this.points.size() - 1;
                for (int n = 1; n < n2 - 1; ++n) {
                    this.segments[n].setXVar(1.0f);
                    this.segments[n].setYVar(4.0f);
                    this.segments[n].setZVar(1.0f);
                    this.segments[n].setSomeVec(new Vec(
                            4.0 * this.points.get(n).x() + 2.0 * this.points.get(n + 1).x(),
                            4.0 * this.points.get(n).y() + 2.0 * this.points.get(n + 1).y(),
                            4.0 * this.points.get(n).z() + 2.0 * this.points.get(n + 1).z()
                    ));
                }
                this.segments[n2 - 1].setXVar(2.0f);
                this.segments[n2 - 1].setYVar(7.0f);
                this.segments[n2 - 1].setZVar(0.0f);
                this.segments[n2 - 1].setSomeVec(new Vec(
                        8.0 * this.points.get(n2 - 1).x() + this.points.get(n2).x(),
                        8.0 * this.points.get(n2 - 1).y() + this.points.get(n2).y(),
                        8.0 * this.points.get(n2 - 1).z() + this.points.get(n2).z()
                ));
                for (int n = 1; n < n2; ++n) {
                    double f = this.segments[n].getXVar() / this.segments[n - 1].getYVar();
                    this.segments[n].setYVar(this.segments[n].getYVar() - f * this.segments[n - 1].getZVar());
                    this.segments[n].setSomeVec(new Vec(
                            this.segments[n].getSomeVec().x() - f * this.segments[n - 1].getSomeVec().x(),
                            this.segments[n].getSomeVec().y() - f * this.segments[n - 1].getSomeVec().y(),
                            this.segments[n].getSomeVec().z() - f * this.segments[n - 1].getSomeVec().z()
                    ));
                }
                this.segments[n2 - 1].setControl1(new Vec(
                        this.segments[n2 - 1].getSomeVec().x() / this.segments[n2 - 1].getYVar(),
                        this.segments[n2 - 1].getSomeVec().y() / this.segments[n2 - 1].getYVar(),
                        this.segments[n2 - 1].getSomeVec().z() / this.segments[n2 - 1].getYVar()
                ));
                for (int n = n2 - 2; n >= 0; --n) {
                    this.segments[n].setControl1(new Vec(
                            (this.segments[n].getSomeVec().x() - this.segments[n].getZVar() * this.segments[n + 1].getControl1().x()) / this.segments[n].getYVar(),
                            (this.segments[n].getSomeVec().y() - this.segments[n].getZVar() * this.segments[n + 1].getControl1().y()) / this.segments[n].getYVar(),
                            (this.segments[n].getSomeVec().z() - this.segments[n].getZVar() * this.segments[n + 1].getControl1().z()) / this.segments[n].getYVar()
                    ));
                }
                for (int n = 0; n < n2 - 1; ++n) {
                    this.segments[n].setControl2(new Vec(
                            2.0 * this.points.get(n + 1).x() - this.segments[n + 1].getControl1().x(),
                            2.0 * this.points.get(n + 1).y() - this.segments[n + 1].getControl1().y(),
                            2.0 * this.points.get(n + 1).z() - this.segments[n + 1].getControl1().z()
                    ));
                }
                this.segments[n2 - 1].setControl2(new Vec(
                        0.5 * (this.points.get(n2).x() + this.segments[n2 - 1].getControl1().x()),
                        0.5 * (this.points.get(n2).y() + this.segments[n2 - 1].getControl1().y()),
                        0.5 * (this.points.get(n2).z() + this.segments[n2 - 1].getControl1().z())
                ));
            }
//        if (d != null) {
//            for (BezierCurve a : this.segments) {
//                a._(d);
//            }
//        }
//        if (d2 != null) {
//            for (BezierCurve a : this.segments) {
//                a.G(d2);
//            }
//        }
//        if (d3 != null) {
//            for (BezierCurve a : this.segments) {
//                a.M(d3);
//            }
//        }
        }

        public class BezierSegment {
            private final Point start, end; // start and end points
            private Point c1 = null, c2 = null; // control points

            private double xVar, yVar, zVar;
            private Point someVec;

            public BezierSegment(@NotNull Point start, @NotNull Point end) {
                this.start = start;
                this.end = end;
            }

            public @NotNull Point getPointAt(double d) {
                return new Vec(
                        bezierPoint(d, start.x(), c1.x(), c2.x(), end.x()),
                        bezierPoint(d, start.y(), c1.y(), c2.y(), end.y()),
                        bezierPoint(d, start.z(), c1.z(), c2.z(), end.z())
                );
            }

            public double length() {
                return start.distance(end);
            }

            public Point getStart() {
                return start;
            }

            public Point getEnd() {
                return end;
            }

            public void setControl1(@NotNull Point c1) {
                this.c1 = c1;
            }

            public Point getControl1() {
                return c1;
            }

            public void setControl2(@NotNull Point c2) {
                this.c2 = c2;
            }

            public Point getControl2() {
                return c2;
            }

            public double getXVar() {
                return xVar;
            }

            public void setXVar(double xVar) {
                this.xVar = xVar;
            }

            public double getYVar() {
                return yVar;
            }

            public void setYVar(double yVar) {
                this.yVar = yVar;
            }

            public double getZVar() {
                return zVar;
            }

            public void setZVar(double zVar) {
                this.zVar = zVar;
            }

            public Point getSomeVec() {
                return someVec;
            }

            public void setSomeVec(Point someVec) {
                this.someVec = someVec;
            }


            private static double bezierPoint(double t, double p0, double p1, double p2, double p3) {
                return p0 * Math.pow(1 - t, 3) + 3 * p1 * Math.pow(1 - t, 2) * t + 3 * p2 * (1 - t) * t * t + p3 * t * t * t;
            }
        }

    }
}
