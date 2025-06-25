package net.hollowcube.common.math;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public record SVD(
        @NotNull Vec translation,
        @NotNull Quaternion leftRotation,
        @NotNull Vec scale,
        @NotNull Quaternion rightRotation
) {
    private static final float G = (float) (3.0F + 2.0F * Math.sqrt(2.0F));
    private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

    public static void main(String[] args) {
        var floats = new float[]{0f, -0.877f, 0f, 1.94875f, 0f, 0f, 0.5f, 2.125f, -0.5f, 0f, 0f, 0.97375f, 0f, 0f, 0f, 1f};
        var mat = new Mat4(floats);
        var svd = mat.svdDecompose();
        System.out.println(svd.translation);
        System.out.println(svd.leftRotation);
        System.out.println(svd.scale);
        System.out.println(svd.rightRotation);
    }

    static @NotNull SVD decompose(@NotNull Mat4 transform) {
        float scale = 1.0F / transform.m33();
        var result = decomposeInternal(new Mat3(transform).scale(scale));
        var translation = transform.translation().mul(scale);
        return new SVD(translation, result.leftRotation, result.scale, result.rightRotation);
    }

    static @NotNull SVD decomposeInternal(@NotNull Mat3 mat) {
        Mat3 copy = new Mat3(mat);
        copy.transpose();
        copy.mul(mat);
        Quaternion quaternion = eigenvalueJacobi(copy, 5);
        float f = copy.m00();
        float g = copy.m11();
        boolean bl = f < 1.0E-6;
        boolean bl2 = g < 1.0E-6;
        Mat3 matrix3f4 = mat.rotate(quaternion);
        Quaternion quaternionf2 = new Quaternion();
        Quaternion quaternionf3 = new Quaternion();
        GivensParameters givensParameters;
        if (bl) {
            givensParameters = qrGivensQuat(matrix3f4.m11(), -matrix3f4.m10());
        } else {
            givensParameters = qrGivensQuat(matrix3f4.m00(), matrix3f4.m01());
        }

        Quaternion quaternionf4 = givensParameters.aroundZ(quaternionf3);
        Mat3 matrix3f5 = givensParameters.aroundZ(copy);
        quaternionf2.mul(quaternionf4);
        matrix3f5.transpose().mul(matrix3f4);
        if (bl) {
            givensParameters = qrGivensQuat(matrix3f5.m22(), -matrix3f5.m20());
        } else {
            givensParameters = qrGivensQuat(matrix3f5.m00(), matrix3f5.m02());
        }

        givensParameters = givensParameters.inverse();
        Quaternion quaternionf5 = givensParameters.aroundY(quaternionf3);
        Mat3 matrix3f6 = givensParameters.aroundY(matrix3f4);
        quaternionf2.mul(quaternionf5);
        matrix3f6.transpose().mul(matrix3f5);
        if (bl2) {
            givensParameters = qrGivensQuat(matrix3f6.m22(), -matrix3f6.m21());
        } else {
            givensParameters = qrGivensQuat(matrix3f6.m11(), matrix3f6.m12());
        }

        Quaternion quaternionf6 = givensParameters.aroundX(quaternionf3);
        Mat3 matrix3f7 = givensParameters.aroundX(matrix3f5);
        quaternionf2.mul(quaternionf6);
        matrix3f7.transpose().mul(matrix3f6);
        Vec vector3f = new Vec(matrix3f7.m00(), matrix3f7.m11(), matrix3f7.m22());
        return new SVD(Vec.ZERO, quaternionf2, vector3f, quaternion.conjugate());
    }

    private static @NotNull Quaternion eigenvalueJacobi(@NotNull Mat3 matrix3f, int i) {
        Quaternion quaternionf = new Quaternion();
        Mat3 matrix3f2 = new Mat3();
        Quaternion quaternionf2 = new Quaternion();

        for (int j = 0; j < i; j++) {
            stepJacobi(matrix3f, matrix3f2, quaternionf2, quaternionf);
        }

        quaternionf.normalize();
        return quaternionf;
    }

    private static void stepJacobi(@NotNull Mat3 matrix3f, @NotNull Mat3 matrix3f2, @NotNull Quaternion quaternionf, @NotNull Quaternion quaternionf2) {
        if (matrix3f.m01() * matrix3f.m01() + matrix3f.m10() * matrix3f.m10() > 1.0E-6F) {
            GivensParameters givensParameters = approxGivensQuat(matrix3f.m00(), 0.5F * (matrix3f.m01() + matrix3f.m10()), matrix3f.m11());
            Quaternion quaternionf3 = givensParameters.aroundZ(quaternionf);
            quaternionf2.mul(quaternionf3);
            givensParameters.aroundZ(matrix3f2);
            similarityTransform(matrix3f, matrix3f2);
        }

        if (matrix3f.m02() * matrix3f.m02() + matrix3f.m20() * matrix3f.m20() > 1.0E-6F) {
            GivensParameters givensParameters = approxGivensQuat(matrix3f.m00(), 0.5F * (matrix3f.m02() + matrix3f.m20()), matrix3f.m22()).inverse();
            Quaternion quaternionf3 = givensParameters.aroundY(quaternionf);
            quaternionf2.mul(quaternionf3);
            givensParameters.aroundY(matrix3f2);
            similarityTransform(matrix3f, matrix3f2);
        }

        if (matrix3f.m12() * matrix3f.m12() + matrix3f.m21() * matrix3f.m21() > 1.0E-6F) {
            GivensParameters givensParameters = approxGivensQuat(matrix3f.m11(), 0.5F * (matrix3f.m12() + matrix3f.m21()), matrix3f.m22());
            Quaternion quaternionf3 = givensParameters.aroundX(quaternionf);
            quaternionf2.mul(quaternionf3);
            givensParameters.aroundX(matrix3f2);
            similarityTransform(matrix3f, matrix3f2);
        }
    }

    private static void similarityTransform(@NotNull Mat3 matrix3f, @NotNull Mat3 matrix3f2) {
        matrix3f.mul(matrix3f2);
        matrix3f2.transpose();
        matrix3f2.mul(matrix3f);
        matrix3f.set(matrix3f2);
    }

    private static GivensParameters qrGivensQuat(float f, float g) {
        float h = (float) java.lang.Math.hypot(f, g);
        float i = h > 1.0E-6F ? g : 0.0F;
        float j = Math.abs(f) + Math.max(h, 1.0E-6F);
        if (f < 0.0F) {
            float k = i;
            i = j;
            j = k;
        }

        return GivensParameters.fromUnnormalized(i, j);
    }

    private static GivensParameters approxGivensQuat(float f, float g, float h) {
        float i = 2.0F * (f - h);
        return G * g * g < i * i ? GivensParameters.fromUnnormalized(g, i) : PI_4;
    }

    private record GivensParameters(float sinHalf, float cosHalf) {
        public static @NotNull GivensParameters fromUnnormalized(float f, float g) {
            float h = MathUtil.invsqrt(f * f + g * g);
            return new GivensParameters(h * f, h * g);
        }

        public static @NotNull GivensParameters fromPositiveAngle(float f) {
            float g = (float) Math.sin(f / 2.0F);
            float h = MathUtil.cosFromSin(g, f / 2.0F);
            return new GivensParameters(g, h);
        }

        public @NotNull GivensParameters inverse() {
            return new GivensParameters(-this.sinHalf, this.cosHalf);
        }

        public @NotNull Quaternion aroundX(@NotNull Quaternion quaternion) {
            return quaternion.set(this.sinHalf, 0.0F, 0.0F, this.cosHalf);
        }

        public @NotNull Quaternion aroundY(@NotNull Quaternion quaternion) {
            return quaternion.set(0.0F, this.sinHalf, 0.0F, this.cosHalf);
        }

        public @NotNull Quaternion aroundZ(@NotNull Quaternion quaternion) {
            return quaternion.set(0.0F, 0.0F, this.sinHalf, this.cosHalf);
        }

        public float cos() {
            return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
        }

        public float sin() {
            return 2.0F * this.sinHalf * this.cosHalf;
        }

        public @NotNull Mat3 aroundX(@NotNull Mat3 mat) {
            mat.m01(0.0F);
            mat.m02(0.0F);
            mat.m10(0.0F);
            mat.m20(0.0F);
            float f = this.cos();
            float g = this.sin();
            mat.m11(f);
            mat.m22(f);
            mat.m12(g);
            mat.m21(-g);
            mat.m00(1.0F);
            return mat;
        }

        public @NotNull Mat3 aroundY(@NotNull Mat3 mat) {
            mat.m01(0.0F);
            mat.m10(0.0F);
            mat.m12(0.0F);
            mat.m21(0.0F);
            float f = this.cos();
            float g = this.sin();
            mat.m00(f);
            mat.m22(f);
            mat.m02(-g);
            mat.m20(g);
            mat.m11(1.0F);
            return mat;
        }

        public @NotNull Mat3 aroundZ(@NotNull Mat3 mat) {
            mat.m02(0.0F);
            mat.m12(0.0F);
            mat.m20(0.0F);
            mat.m21(0.0F);
            float f = this.cos();
            float g = this.sin();
            mat.m00(f);
            mat.m11(f);
            mat.m01(g);
            mat.m10(-g);
            mat.m22(1.0F);
            return mat;
        }
    }
}
