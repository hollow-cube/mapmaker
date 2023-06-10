package net.hollowcube.common.physics;

public final class SweepResult {
    Object obj;
    double res;
    double normalX, normalY, normalZ;

    /**
     * Store the result of a movement operation
     *
     * @param res     Percentage of move completed
     * @param normalX -1 if intersected on left, 1 if intersected on right
     * @param normalY -1 if intersected on bottom, 1 if intersected on top
     * @param normalZ -1 if intersected on front, 1 if intersected on back
     * @param o
     */
    public SweepResult(double res, double normalX, double normalY, double normalZ, Object o) {
        this.res = res;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.obj = o;
    }

    public Object obj() {
        return obj;
    }
}
