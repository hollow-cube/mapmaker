package net.hollowcube.terraform.util;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;

/// An affine transform: a 3x3 linear part plus a translation. It moves cells, and block states reorient
/// through the linear part.
///
/// Rotations are clockwise viewed from the positive axis toward the origin, so [#rotateY] by 90 degrees
/// turns a south-facing block west, like Minecraft. Multiples of 90 degrees have exact 0, 1 and -1
/// entries, so they stay on the block grid.
public record Transform(
    double m00,
    double m01,
    double m02,
    double m03,
    double m10,
    double m11,
    double m12,
    double m13,
    double m20,
    double m21,
    double m22,
    double m23
) {
    public static Transform identity() {
        return new Transform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
    }

    public static Transform translate(double x, double y, double z) {
        return new Transform(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z);
    }

    /// A reflection across the plane normal to `axis`.
    public static Transform mirror(Axis axis) {
        return switch (axis) {
            case X -> new Transform(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            case Y -> new Transform(1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 1, 0);
            case Z -> new Transform(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, 0);
        };
    }

    /// Clockwise viewed from +X toward the origin.
    public static Transform rotateX(double degrees) {
        double c = cos(degrees), s = sin(degrees);
        return new Transform(1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0);
    }

    /// Clockwise viewed from above.
    public static Transform rotateY(double degrees) {
        double c = cos(degrees), s = sin(degrees);
        return new Transform(c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0);
    }

    /// Clockwise viewed from +Z toward the origin.
    public static Transform rotateZ(double degrees) {
        double c = cos(degrees), s = sin(degrees);
        return new Transform(c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0);
    }

    /// The linear part only (ignores translation), for direction vectors.
    public Vec applyDirection(double x, double y, double z) {
        return new Vec(
            m00 * x + m01 * y + m02 * z,
            m10 * x + m11 * y + m12 * z,
            m20 * x + m21 * y + m22 * z
        );
    }

    /// Maps a cell to the cell holding its transformed centre. A bijection of the block grid for a
    /// [cube symmetry][#isCubeSymmetry()].
    public BlockVec applyBlock(int x, int y, int z) {
        double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;
        return new BlockVec(
            (int) Math.floor(m00 * cx + m01 * cy + m02 * cz + m03),
            (int) Math.floor(m10 * cx + m11 * cy + m12 * cz + m13),
            (int) Math.floor(m20 * cx + m21 * cy + m22 * cz + m23)
        );
    }

    /// Applies `first`, then this.
    public Transform compose(Transform first) {
        return new Transform(
            m00 * first.m00 + m01 * first.m10 + m02 * first.m20,
            m00 * first.m01 + m01 * first.m11 + m02 * first.m21,
            m00 * first.m02 + m01 * first.m12 + m02 * first.m22,
            m00 * first.m03 + m01 * first.m13 + m02 * first.m23 + m03,
            m10 * first.m00 + m11 * first.m10 + m12 * first.m20,
            m10 * first.m01 + m11 * first.m11 + m12 * first.m21,
            m10 * first.m02 + m11 * first.m12 + m12 * first.m22,
            m10 * first.m03 + m11 * first.m13 + m12 * first.m23 + m13,
            m20 * first.m00 + m21 * first.m10 + m22 * first.m20,
            m20 * first.m01 + m21 * first.m11 + m22 * first.m21,
            m20 * first.m02 + m21 * first.m12 + m22 * first.m22,
            m20 * first.m03 + m21 * first.m13 + m22 * first.m23 + m23
        );
    }

    public boolean isIdentity() {
        return isLinearIdentity() && near(m03, 0) && near(m13, 0) && near(m23, 0);
    }

    /// Whether the linear part is the identity, so block states need no reorienting.
    public boolean isLinearIdentity() {
        return near(m00, 1)
            && near(m01, 0)
            && near(m02, 0)
            && near(m10, 0)
            && near(m11, 1)
            && near(m12, 0)
            && near(m20, 0)
            && near(m21, 0)
            && near(m22, 1);
    }

    /// Whether the linear part is a signed permutation matrix (every row and column has one 1 or -1
    /// entry): the 48 rotations and reflections of a cube, for which [#applyBlock] is a bijection.
    public boolean isCubeSymmetry() {
        double[] linear = {m00, m01, m02, m10, m11, m12, m20, m21, m22};
        int[] rows = new int[3], columns = new int[3];
        for (int i = 0; i < 9; i++) {
            double v = Math.abs(linear[i]);
            if (v < Vec.EPSILON) continue;
            if (!near(v, 1)) return false;
            rows[i / 3]++;
            columns[i % 3]++;
        }
        for (int i = 0; i < 3; i++) {
            if (rows[i] != 1 || columns[i] != 1) return false;
        }
        return true;
    }

    /// Whether the x-z part is a reflection, which swaps chiral states (stair corners, door hinges).
    public boolean isHorizontalFlip() {
        return m00 * m22 - m02 * m20 < -Vec.EPSILON;
    }

    /// Whether y is reflected, which swaps top and bottom states.
    public boolean isVerticalFlip() {
        return m11 < -Vec.EPSILON;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < Vec.EPSILON;
    }

    // Math.cos(toRadians(90)) is ~6e-17, which would push a quarter turn off the block grid, so the
    // cardinals are exact.
    private static double cos(double degrees) {
        double d = normalize(degrees);
        if (d == 0) return 1;
        if (d == 90 || d == 270) return 0;
        if (d == 180) return -1;
        return Math.cos(Math.toRadians(d));
    }

    private static double sin(double degrees) {
        double d = normalize(degrees);
        if (d == 0 || d == 180) return 0;
        if (d == 90) return 1;
        if (d == 270) return -1;
        return Math.sin(Math.toRadians(d));
    }

    private static double normalize(double degrees) {
        double d = degrees % 360;
        return d < 0 ? d + 360 : d;
    }

    public enum Axis {
        X,
        Y,
        Z,
    }
}
