package net.hollowcube.terraform.cui.vanilla.lines;

import net.hollowcube.schem.util.Axis;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AxisAlignedLine extends AbstractLine {
    public AxisAlignedLine(
            Player player,
            Point from,
            Point to,
            RGBLike color
    ) {
        super(player, from, toAxisPoint(from, to), color);
    }

    private static @NotNull Point toAxisPoint(Point from, Point to) {
        return from.add(getVec(Vec.fromPoint(to.sub(from))));
    }

    private static @NotNull Vec getVec(Vec vec) {
        var axis = Axis.X;
        var biggest = Math.abs(vec.x());

        if (Math.abs(vec.y()) > biggest) {
            axis = Axis.Y;
            biggest = Math.abs(vec.y());
        }
        if (Math.abs(vec.z()) > biggest) {
            axis = Axis.Z;
        }

        return switch (axis) {
            case X -> new Vec(vec.x(), 0, 0);
            case Y -> new Vec(0, vec.y(), 0);
            case Z -> new Vec(0, 0, vec.z());
        };
    }

    @Override
    public void reshape(Point from, Point to) {
        super.reshape(from, toAxisPoint(from, to));
    }
}
