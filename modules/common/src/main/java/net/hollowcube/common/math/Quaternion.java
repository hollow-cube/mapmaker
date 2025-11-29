package net.hollowcube.common.math;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public final class Quaternion {
    public static final float[] ZERO = new float[]{0, 0, 0, 1};
    public static final NetworkBuffer.Type<Quaternion> FLOAT_NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.FLOAT, i -> (float) i.getX(),
            NetworkBuffer.FLOAT, i -> (float) i.getY(),
            NetworkBuffer.FLOAT, i -> (float) i.getZ(),
            NetworkBuffer.FLOAT, i -> (float) i.getW(),
            Quaternion::new
    );

    private double x;
    private double y;
    private double z;
    private double w;

    public Quaternion() {
        this(0, 0, 0, 1);
    }

    public Quaternion(final Quaternion q) {
        this(q.x, q.y, q.z, q.w);
    }

    public Quaternion(final float[] q) {
        this(q[0], q[1], q[2], q[3]);
    }

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void set(final Quaternion q) {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w;
    }

    public @NotNull Quaternion set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Quaternion(Vec axis, double angle) {
        set(axis, angle);
    }

    public double norm() {
        return Math.sqrt(dot(this));
    }

    public double w() {
        return w;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double getW() {
        return w;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /**
     * @param axis  rotation axis, unit vector
     * @param angle the rotation angle
     * @return this
     */
    public Quaternion set(Vec axis, double angle) {
        double s = Math.sin(angle / 2);
        w = Math.cos(angle / 2);
        x = axis.x() * s;
        y = axis.y() * s;
        z = axis.z() * s;
        return this;
    }

    public Quaternion mul(Quaternion q) {
        return set(
                (float) Math.fma(this.w, q.x(), Math.fma(this.x, q.w(), Math.fma(this.y, q.z(), -this.z * q.y()))),
                (float) Math.fma(this.w, q.y(), Math.fma(-this.x, q.z(), Math.fma(this.y, q.w(), this.z * q.x()))),
                (float) Math.fma(this.w, q.z(), Math.fma(this.x, q.y(), Math.fma(-this.y, q.x(), this.z * q.w()))),
                (float) Math.fma(this.w, q.w(), Math.fma(-this.x, q.x(), Math.fma(-this.y, q.y(), -this.z * q.z())))
        );
    }

    public double[] toRotationMatrix() {
        var matrix = new double[9];

        var s = Math.pow(Math.sqrt(x * x + y * y + z * z + w * w), -2);


        matrix[0] = (1 - 2 * s * (Math.pow(y, 2) + Math.pow(z, 2)));
        matrix[1] = 2 * s * (x * y - z * w);
        matrix[2] = 2 * s * (x * z + y * w);

        matrix[3] = 2 * s * (x * y + z * w);
        matrix[4] = (1 - 2 * s * (Math.pow(x, 2) + Math.pow(z, 2)));
        matrix[5] = 2 * s * (z * y - x * w);

        matrix[6] = 2 * s * (x * z - y * w);
        matrix[7] = 2 * s * (y * z + x * w);
        matrix[8] = (1 - 2 * s * (Math.pow(x, 2) + Math.pow(y, 2)));

        return matrix;
    }

    public @NotNull Quaternion conjugate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = this.w;
        return this;
    }

    public Quaternion normalize() {
        float invNorm = MathUtil.invsqrt((float) Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w))));
        this.x = this.x * invNorm;
        this.y = this.y * invNorm;
        this.z = this.z * invNorm;
        this.w = this.w * invNorm;
        return this;
    }

    public Quaternion mulThis(Quaternion q) {
        double nw = w * q.w - x * q.x - y * q.y - z * q.z;
        double nx = w * q.x + x * q.w + y * q.z - z * q.y;
        double ny = w * q.y + y * q.w + z * q.x - x * q.z;
        z = w * q.z + z * q.w + x * q.y - y * q.x;
        w = nw;
        x = nx;
        y = ny;
        return this;
    }

    public Quaternion scaleThis(double scale) {
        if (scale != 1) {
            w *= scale;
            x *= scale;
            y *= scale;
            z *= scale;
        }
        return this;
    }

    public Quaternion divThis(double scale) {
        if (scale != 1) {
            w /= scale;
            x /= scale;
            y /= scale;
            z /= scale;
        }
        return this;
    }

    public double dot(Quaternion q) {
        return x * q.x + y * q.y + z * q.z + w * q.w;
    }

    public boolean equals(Quaternion q) {
        return x == q.x && y == q.y && z == q.z && w == q.w;
    }

    public float[] into() {
        return new float[]{(float) x, (float) y, (float) z, (float) w};
    }

    public Quaternion interpolateThis(Quaternion q, double t) {
        if (!equals(q)) {
            double d = dot(q);
            double qx, qy, qz, qw;

            if (d < 0f) {
                qx = -q.x;
                qy = -q.y;
                qz = -q.z;
                qw = -q.w;
                d = -d;
            } else {
                qx = q.x;
                qy = q.y;
                qz = q.z;
                qw = q.w;
            }

            double f0, f1;

            if ((1 - d) > 0.1f) {
                double angle = (double) Math.acos(d);
                double s = (double) Math.sin(angle);
                double tAngle = t * angle;
                f0 = (double) Math.sin(angle - tAngle) / s;
                f1 = (double) Math.sin(tAngle) / s;
            } else {
                f0 = 1 - t;
                f1 = t;
            }

            x = f0 * x + f1 * qx;
            y = f0 * y + f1 * qy;
            z = f0 * z + f1 * qz;
            w = f0 * w + f1 * qw;
        }

        return this;
    }

    public Quaternion normalizeThis() {
        return divThis(norm());
    }

    public Quaternion interpolate(Quaternion q, double t) {
        return new Quaternion(this).interpolateThis(q, t);
    }

    /**
     * Converts this Quaternion into a matrix, returning it as a float array.
     */
    public float[] toMatrix() {
        float[] matrixs = new float[16];
        toMatrix(matrixs);
        return matrixs;
    }

    /**
     * Converts this Quaternion into a matrix, placing the values into the given array.
     *
     * @param matrixs 16-length float array.
     */
    public final void toMatrix(float[] matrixs) {
        matrixs[3] = 0.0f;
        matrixs[7] = 0.0f;
        matrixs[11] = 0.0f;
        matrixs[12] = 0.0f;
        matrixs[13] = 0.0f;
        matrixs[14] = 0.0f;
        matrixs[15] = 1.0f;

        matrixs[0] = (float) (1.0f - (2.0f * ((y * y) + (z * z))));
        matrixs[1] = (float) (2.0f * ((x * y) - (z * w)));
        matrixs[2] = (float) (2.0f * ((x * z) + (y * w)));

        matrixs[4] = (float) (2.0f * ((x * y) + (z * w)));
        matrixs[5] = (float) (1.0f - (2.0f * ((x * x) + (z * z))));
        matrixs[6] = (float) (2.0f * ((y * z) - (x * w)));

        matrixs[8] = (float) (2.0f * ((x * z) - (y * w)));
        matrixs[9] = (float) (2.0f * ((y * z) + (x * w)));
        matrixs[10] = (float) (1.0f - (2.0f * ((x * x) + (y * y))));
    }

    public Point toEulerAngles() {
        return new Vec(
                Math.toDegrees(Math.asin(-2 * (this.y * this.z - this.w * this.x))), // pitch
                Math.toDegrees(Math.atan2(2 * (this.x * this.z + this.w * this.y), this.w * this.w - this.x * this.x - this.y * this.y + this.z * this.z)), // yaw
                Math.toDegrees(Math.atan2(2 * (this.x * this.y + this.w * this.z), this.w * this.w - this.x * this.x + this.y * this.y - this.z * this.z)) // roll
        );
    }

    public static Quaternion fromEulerAngles(Point angles) {
        return new Quaternion(new Vec(1, 0, 0), Math.toRadians(angles.x()))
                .mulThis(new Quaternion(new Vec(0, 1, 0), Math.toRadians(angles.y())))
                .mulThis(new Quaternion(new Vec(0, 0, 1), Math.toRadians(angles.z())));
    }

    @Override
    public String toString() {
        return "Quaternion{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }
}
