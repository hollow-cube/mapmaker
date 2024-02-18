package net.hollowcube.mapmaker.map.feature.experimental.pathtool;

import com.mattworzala.debug.DebugMessage;
import com.mattworzala.debug.shape.Shape;
import net.hollowcube.common.physics.BoundingBox;
import net.hollowcube.common.physics.RayUtils;
import net.hollowcube.common.physics.SweepResult;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PathPoint {
    private final PathData path;
    private final Point point;

    private final Vec bbStart;
    private final BoundingBox bb;

    public PathPoint(@NotNull PathData path, Point point) {
        this.path = path;
        this.point = CoordinateUtil.floor(point);

        this.bb = new BoundingBox(
                0.4, 0.4, 0.4
//                this.pos.add(0.4, 0.4, 0.4)////////
        );
        this.bbStart = Vec.fromPoint(this.point).add(0.5, 0.3, 0.5);
    }

    public @NotNull Point point() {
        return point;
    }

    private boolean selected = false;

    public boolean updateLookingAt(Player player, boolean ignoreStart) {
        var start = player.getPosition().add(0, player.getEyeHeight(), 0).asVec();
        var direction = player.getPosition().direction();

        SweepResult res = new SweepResult(Double.MAX_VALUE, 0, 0, 0, null);

        var oldValue = selected;

        // Calculate faces
        if (bb.containsPoint(start)) return !ignoreStart;
        selected = RayUtils.BoundingBoxRayIntersectionCheck(start, direction.mul(50), bb, bbStart, res, null);

        if (selected != oldValue) {
            path.updateRendering();
        }
        return selected;
    }

    public void draw(@NotNull DebugMessage.Builder builder, String namespace) {
        builder.set(namespace + ":point_" + point.hashCode(), Shape.box()
                .start(point.add(0.3))
                .end(point.add(0.7))
                .faceColor(selected ? 0x5500FF00 : 0x55FF0000)
                .edgeColor(0xFFFF0000)
                .build());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPoint pathPoint = (PathPoint) o;
        return Objects.equals(point, pathPoint.point);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point);
    }
}
