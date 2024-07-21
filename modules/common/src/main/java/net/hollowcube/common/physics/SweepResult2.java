package net.hollowcube.common.physics;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public final class SweepResult2 {

    double res;
    double normalX, normalY, normalZ;
    double collidedPositionX, collidedPositionY, collidedPositionZ;

    public SweepResult2() {
        this(Double.MAX_VALUE, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Store the result of a movement operation
     *
     * @param res     Percentage of move completed
     * @param normalX -1 if intersected on left, 1 if intersected on right
     * @param normalY -1 if intersected on bottom, 1 if intersected on top
     * @param normalZ -1 if intersected on front, 1 if intersected on back
     */
    public SweepResult2(double res, double normalX, double normalY, double normalZ, double collidedPosX, double collidedPosY, double collidedPosZ) {
        this.res = res;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.collidedPositionX = collidedPosX;
        this.collidedPositionY = collidedPosY;
        this.collidedPositionZ = collidedPosZ;
    }

    public double collidedPositionX() {
        return collidedPositionX;
    }

    public double collidedPositionY() {
        return collidedPositionY;
    }

    public double collidedPositionZ() {
        return collidedPositionZ;
    }

    public @NotNull Point getCollidedPosition() {
        return new Vec(collidedPositionX, collidedPositionY, collidedPositionZ);
    }
}
