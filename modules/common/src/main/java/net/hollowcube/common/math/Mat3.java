package net.hollowcube.common.math;

import org.jetbrains.annotations.NotNull;

public class Mat3 {
    private float m00;
    private float m01;
    private float m02;
    private float m10;
    private float m11;
    private float m12;
    private float m20;
    private float m21;
    private float m22;

    public Mat3() {
        this.m00 = 1.0F;
        this.m11 = 1.0F;
        this.m22 = 1.0F;
    }

    public Mat3(@NotNull Mat3 mat) {
        this.m00 = mat.m00;
        this.m01 = mat.m01;
        this.m02 = mat.m02;
        this.m10 = mat.m10;
        this.m11 = mat.m11;
        this.m12 = mat.m12;
        this.m20 = mat.m20;
        this.m21 = mat.m21;
        this.m22 = mat.m22;
    }

    public Mat3(@NotNull Mat4 mat) {
        this.m00 = mat.m00();
        this.m01 = mat.m01();
        this.m02 = mat.m02();
        this.m10 = mat.m10();
        this.m11 = mat.m11();
        this.m12 = mat.m12();
        this.m20 = mat.m20();
        this.m21 = mat.m21();
        this.m22 = mat.m22();
    }

    public float m00() {
        return this.m00;
    }

    public float m01() {
        return this.m01;
    }

    public float m02() {
        return this.m02;
    }

    public float m10() {
        return this.m10;
    }

    public float m11() {
        return this.m11;
    }

    public float m12() {
        return this.m12;
    }

    public float m20() {
        return this.m20;
    }

    public float m21() {
        return this.m21;
    }

    public float m22() {
        return this.m22;
    }

    public @NotNull Mat3 m00(float m00) {
        this.m00 = m00;
        return this;
    }

    public @NotNull Mat3 m01(float m01) {
        this.m01 = m01;
        return this;
    }

    public @NotNull Mat3 m02(float m02) {
        this.m02 = m02;
        return this;
    }

    public @NotNull Mat3 m10(float m10) {
        this.m10 = m10;
        return this;
    }

    public @NotNull Mat3 m11(float m11) {
        this.m11 = m11;
        return this;
    }

    public @NotNull Mat3 m12(float m12) {
        this.m12 = m12;
        return this;
    }

    public @NotNull Mat3 m20(float m20) {
        this.m20 = m20;
        return this;
    }

    public @NotNull Mat3 m21(float m21) {
        this.m21 = m21;
        return this;
    }

    public @NotNull Mat3 m22(float m22) {
        this.m22 = m22;
        return this;
    }

    public @NotNull Mat3 set(Mat3 m) {
        return m == this ? this : this
                .m00(m.m00()).m01(m.m01()).m02(m.m02())
                .m10(m.m10()).m11(m.m11()).m12(m.m12())
                .m20(m.m20()).m21(m.m21()).m22(m.m22());
    }

    public @NotNull Mat3 set(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        return this;
    }

    public @NotNull Mat3 mul(Mat3 right) {
        float nm00 = Math.fma(m00, right.m00(), Math.fma(m10, right.m01(), m20 * right.m02()));
        float nm01 = Math.fma(m01, right.m00(), Math.fma(m11, right.m01(), m21 * right.m02()));
        float nm02 = Math.fma(m02, right.m00(), Math.fma(m12, right.m01(), m22 * right.m02()));
        float nm10 = Math.fma(m00, right.m10(), Math.fma(m10, right.m11(), m20 * right.m12()));
        float nm11 = Math.fma(m01, right.m10(), Math.fma(m11, right.m11(), m21 * right.m12()));
        float nm12 = Math.fma(m02, right.m10(), Math.fma(m12, right.m11(), m22 * right.m12()));
        float nm20 = Math.fma(m00, right.m20(), Math.fma(m10, right.m21(), m20 * right.m22()));
        float nm21 = Math.fma(m01, right.m20(), Math.fma(m11, right.m21(), m21 * right.m22()));
        float nm22 = Math.fma(m02, right.m20(), Math.fma(m12, right.m21(), m22 * right.m22()));
        return this.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22);
    }

    public @NotNull Mat3 rotate(@NotNull Quaternion quat) {
        float w2 = (float) (quat.w() * quat.w());
        float x2 = (float) (quat.x() * quat.x());
        float y2 = (float) (quat.y() * quat.y());
        float z2 = (float) (quat.z() * quat.z());
        float zw = (float) (quat.z() * quat.w());
        float dzw = zw + zw;
        float xy = (float) (quat.x() * quat.y());
        float dxy = xy + xy;
        float xz = (float) (quat.x() * quat.z());
        float dxz = xz + xz;
        float yw = (float) (quat.y() * quat.w());
        float dyw = yw + yw;
        float yz = (float) (quat.y() * quat.z());
        float dyz = yz + yz;
        float xw = (float) (quat.x() * quat.w());
        float dxw = xw + xw;
        float rm00 = w2 + x2 - z2 - y2;
        float rm01 = dxy + dzw;
        float rm02 = dxz - dyw;
        float rm10 = dxy - dzw;
        float rm11 = y2 - z2 + w2 - x2;
        float rm12 = dyz + dxw;
        float rm20 = dyw + dxz;
        float rm21 = dyz - dxw;
        float rm22 = z2 - y2 - x2 + w2;
        float nm00 = this.m00 * rm00 + this.m10 * rm01 + this.m20 * rm02;
        float nm01 = this.m01 * rm00 + this.m11 * rm01 + this.m21 * rm02;
        float nm02 = this.m02 * rm00 + this.m12 * rm01 + this.m22 * rm02;
        float nm10 = this.m00 * rm10 + this.m10 * rm11 + this.m20 * rm12;
        float nm11 = this.m01 * rm10 + this.m11 * rm11 + this.m21 * rm12;
        float nm12 = this.m02 * rm10 + this.m12 * rm11 + this.m22 * rm12;
        this.m20 = this.m00 * rm20 + this.m10 * rm21 + this.m20 * rm22;
        this.m21 = this.m01 * rm20 + this.m11 * rm21 + this.m21 * rm22;
        this.m22 = this.m02 * rm20 + this.m12 * rm21 + this.m22 * rm22;
        this.m00 = nm00;
        this.m01 = nm01;
        this.m02 = nm02;
        this.m10 = nm10;
        this.m11 = nm11;
        this.m12 = nm12;
        return this;
    }

    public @NotNull Mat3 scale(float xyz) {
        return scale(xyz, xyz, xyz);
    }

    public @NotNull Mat3 scale(float x, float y, float z) {
        m00 *= x;
        m01 *= x;
        m02 *= x;
        m10 *= y;
        m11 *= y;
        m12 *= y;
        m20 *= z;
        m21 *= z;
        m22 *= z;
        return this;
    }

    public @NotNull Mat3 transpose() {
        return this.set(m00, m10, m20,
                m01, m11, m21,
                m02, m12, m22);
    }

}
