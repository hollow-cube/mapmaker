package net.hollowcube.map.feature.experimental.pathtool;

import com.mattworzala.debug.DebugMessage;
import com.mattworzala.debug.shape.LineShape;
import com.mattworzala.debug.shape.Shape;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathData implements Viewable { //todo maybe should be Viewable?
    private final String id;
    private final List<PathPoint> points = new ArrayList<>();

    private Set<Player> viewers = new HashSet<>();

    public PathData(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }

    public void reset() {
        points.clear();
        updateRendering();
    }

    public boolean addPoint(@NotNull Point point) {
        var pathPoint = new PathPoint(this, point);
        if (points.contains(pathPoint)) return false;

        points.add(pathPoint);
        updateRendering();
        return true;
    }

    private int selectedLine = -1;

    public void updateRendering() {
        var namespace = "path_tool_" + id;
        var builder = DebugMessage.builder()
                .clear(namespace);

        for (var point : points) {
            point.draw(builder, namespace);
//            lineBuilder.pos(pos.pos().add(0.5));
        }

        if (points.size() > 1) {
            for (int i = 1; i < points.size(); i++) {
                var start = points.get(i - 1).point().add(0.5);
                var end = points.get(i).point().add(0.5);
                builder.set(namespace + ":line_" + i, Shape.line()
                        .type(LineShape.Type.SINGLE)
                        .lineWidth(4f)
                        .color(i == selectedLine ? 0xFF00FF00 : 0xFFFF0000)
                        .point(start)
                        .point(end)
                        .build());
            }
        }

        sendPacketToViewers(builder.build().getPacket());
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        viewers.add(player);
        return true;
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        return viewers.remove(player);
    }

    @Override
    public @NotNull Set<@NotNull Player> getViewers() {
        return Set.copyOf(viewers);
    }

    public void tick() {
        var viewer = viewers.stream().findFirst().orElse(null);
        if (viewer == null) return;

        var oldSelected = selectedLine;

        selectedLine = -1;

        boolean found = false;
        for (var point : points) {
            found |= point.updateLookingAt(viewer, false);
        }
        if (points.size() > 1 && !found) {
            for (int i = 1; i < points.size(); i++) {
                var start = viewer.getPosition().add(0, viewer.getEyeHeight(), 0).asVec();
                var direction = viewer.getPosition().direction().mul(50);

                var lineStart = Vec.fromPoint(points.get(i - 1).point()).add(0.5);
                var lineEnd = Vec.fromPoint(points.get(i).point()).add(0.5);

                var radius = 5;

                var dist = closestPointsDistance(start, start.add(direction), lineStart, lineEnd);
                if (dist < 0.2) {
                    selectedLine = i;
                    break;
                }
//                System.out.println(dist);
//                if (lineCylinderIntersection(start, direction, lineStart, lineDirection, radius)) {
//                    System.out.println("intersection");
//                }

            }

            if (oldSelected != selectedLine) {
                updateRendering();
            }
        }
    }

    public static boolean lineCylinderIntersection(Vec vecStart, Vec vecDirection, Vec cylStart, Vec cylDirection, double radius) {

//        Vec startToEnd = vecStart.sub(cylStart);
//        double dotDirection = vecDirection.dot(cylDirection);
//        double dotStartToEnd = startToEnd.dot(cylDirection);
//
//        double a = vecDirection.dot(vecDirection) - dotDirection * dotDirection;
//        double b = 2 * (vecDirection.dot(startToEnd) - dotDirection * dotStartToEnd);
//        double c = startToEnd.dot(startToEnd) - dotStartToEnd * dotStartToEnd - radius * radius;
//
//        double discriminant = b * b - 4 * a * c;
//
//        if (discriminant < 0) {
//            // No intersection
//            return false;
//        } else if (discriminant == 0) {
//            // One intersection
//            double t = -b / (2 * a);
//            return t >= 0 && t <= 1;
//        } else {
//            // Two intersections
//            double t1 = (-b - Math.sqrt(discriminant)) / (2 * a);
//            double t2 = (-b + Math.sqrt(discriminant)) / (2 * a);
//            return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1);
//        }

//        // Calculate the closest pos on the line to the cylinder's start
//        double projection = cylDirection.dot(vecStart.sub(cylStart));
//        Vec closestPoint = cylStart.add(cylDirection.mul(projection));
//
//        // Calculate the distance between the closest pos and the cylinder's start
//        double distanceToStart = closestPoint.distance(cylStart);
//
//        // Check if the closest pos is within the cylinder's radius
//        if (distanceToStart <= radius) {
//            // Check if the intersection pos is within the cylinder's height
//            double vecLength = vecDirection.length();
//            double t = projection / vecLength;
//            double height = cylDirection.length();
//            if (t >= 0 && t <= height) {
//                return true;
//            }
//        }
//        return false;

        vecDirection = vecDirection.normalize();
        cylDirection = cylDirection.normalize();

        Vec startToEnd = vecStart.sub(cylStart);
        double dotDirection = vecDirection.dot(cylDirection);
        double dotStartToEnd = startToEnd.dot(cylDirection);

        double a = vecDirection.dot(vecDirection) - dotDirection * dotDirection;
        double b = 2 * (vecDirection.dot(startToEnd) - dotDirection * dotStartToEnd);
        double c = startToEnd.dot(startToEnd) - dotStartToEnd * dotStartToEnd - radius * radius;

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // No intersection
            return false;
        } else if (discriminant == 0) {
            // One intersection
            double t = -b / (2 * a);
            return t >= 0 && t <= 1;
        } else {
            // Two intersections
            double t1 = (-b - Math.sqrt(discriminant)) / (2 * a);
            double t2 = (-b + Math.sqrt(discriminant)) / (2 * a);
            return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1);
        }
    }

    public static double closestPointsDistance(Vec line1Start, Vec line1End, Vec line2Start, Vec line2End) {
        Vec u = line1End.sub(line1Start);
        Vec v = line2End.sub(line2Start);
        Vec w = line1Start.sub(line2Start);

        double a = u.dot(u);
        double b = u.dot(v);
        double c = v.dot(v);
        double d = u.dot(w);
        double e = v.dot(w);

        double denominator = a * c - b * b;
        if (denominator == 0) {
            // Lines are parallel, return the distance between the start points
            return w.length();
        }

        double t1 = (b * e - c * d) / denominator;
        double t2 = (a * e - b * d) / denominator;

        // Clamp the values of t1 and t2 to the range [0, 1]
        t1 = Math.max(0, Math.min(1, t1));
        t2 = Math.max(0, Math.min(1, t2));

        Vec closestPointLine1 = line1Start.add(u.mul(t1));
        Vec closestPointLine2 = line2Start.add(v.mul(t2));
        return closestPointLine1.sub(closestPointLine2).length();

//        Vec u = line1End.sub(line1Start);
//        Vec v = line2End.sub(line2Start);
//        Vec w = line1Start.sub(line2Start);
//
//        double a = u.dot(u);
//        double b = u.dot(v);
//        double c = v.dot(v);
//        double d = u.dot(w);
//        double e = v.dot(w);
//
//        double denominator = a * c - b * b;
//        if (denominator == 0) {
//            // Lines are parallel, return the distance between the start points
//            return w.length();
//        }
//
//        double t1 = (b * e - c * d) / denominator;
//        double t2 = (a * e - b * d) / denominator;
//
//        if (t1 < 0) {
//            // Closest pos on line 1 is before the start pos
//            return w.length();
//        } else if (t1 > 1) {
//            // Closest pos on line 1 is after the end pos
//            Vec v2 = line2End.sub(line1End);
//            return v2.length();
//        } else {
//            // Closest pos on line 1 is within the line segment
//            Vec closestPointLine1 = line1Start.add(u.mul(t1));
//            Vec closestPointLine2 = line2Start.add(v.mul(t2));
//            return closestPointLine1.sub(closestPointLine2).length();
//        }
    }
}
