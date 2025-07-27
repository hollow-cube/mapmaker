package net.hollowcube.common.math;

import net.hollowcube.common.util.FloatIndexer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public class Mat4 {
    private float m00;
    private float m01;
    private float m02;
    private float m03;
    private float m10;
    private float m11;
    private float m12;
    private float m13;
    private float m20;
    private float m21;
    private float m22;
    private float m23;
    private float m30;
    private float m31;
    private float m32;
    private float m33;

    public Mat4() {
        this.m00 = 1.0F;
        this.m11 = 1.0F;
        this.m22 = 1.0F;
        this.m33 = 1.0F;
    }

    public Mat4(float[] floats) {
        Check.argCondition(floats.length != 16, "Array must have exactly 16 elements");
        this.m00 = floats[0];
        this.m01 = floats[4];
        this.m02 = floats[8];
        this.m03 = floats[12];
        this.m10 = floats[1];
        this.m11 = floats[5];
        this.m12 = floats[9];
        this.m13 = floats[13];
        this.m20 = floats[2];
        this.m21 = floats[6];
        this.m22 = floats[10];
        this.m23 = floats[14];
        this.m30 = floats[3];
        this.m31 = floats[7];
        this.m32 = floats[11];
        this.m33 = floats[15];
    }

    public Mat4(@NotNull FloatIndexer floats) {
        this.m00 = floats.get(0);
        this.m01 = floats.get(4);
        this.m02 = floats.get(8);
        this.m03 = floats.get(12);
        this.m10 = floats.get(1);
        this.m11 = floats.get(5);
        this.m12 = floats.get(9);
        this.m13 = floats.get(13);
        this.m20 = floats.get(2);
        this.m21 = floats.get(6);
        this.m22 = floats.get(10);
        this.m23 = floats.get(14);
        this.m30 = floats.get(3);
        this.m31 = floats.get(7);
        this.m32 = floats.get(11);
        this.m33 = floats.get(15);
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

    public float m03() {
        return this.m03;
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

    public float m13() {
        return this.m13;
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

    public float m23() {
        return this.m23;
    }

    public float m30() {
        return this.m30;
    }

    public float m31() {
        return this.m31;
    }

    public float m32() {
        return this.m32;
    }

    public float m33() {
        return this.m33;
    }

    public @NotNull Mat4 m00(float m00) {
        this.m00 = m00;
        return this;
    }

    public @NotNull Mat4 m01(float m01) {
        this.m01 = m01;
        return this;
    }

    public @NotNull Mat4 m02(float m02) {
        this.m02 = m02;
        return this;
    }

    public @NotNull Mat4 m03(float m03) {
        this.m03 = m03;
        return this;
    }

    public @NotNull Mat4 m10(float m10) {
        this.m10 = m10;
        return this;
    }

    public @NotNull Mat4 m11(float m11) {
        this.m11 = m11;
        return this;
    }

    public @NotNull Mat4 m12(float m12) {
        this.m12 = m12;
        return this;
    }

    public @NotNull Mat4 m13(float m13) {
        this.m13 = m13;
        return this;
    }

    public @NotNull Mat4 m20(float m20) {
        this.m20 = m20;
        return this;
    }

    public @NotNull Mat4 m21(float m21) {
        this.m21 = m21;
        return this;
    }

    public @NotNull Mat4 m22(float m22) {
        this.m22 = m22;
        return this;
    }

    public @NotNull Mat4 m23(float m23) {
        this.m23 = m23;
        return this;
    }

    public @NotNull Mat4 m30(float m30) {
        this.m30 = m30;
        return this;
    }

    public @NotNull Mat4 m31(float m31) {
        this.m31 = m31;
        return this;
    }

    public @NotNull Mat4 m32(float m32) {
        this.m32 = m32;
        return this;
    }

    public @NotNull Mat4 m33(float m33) {
        this.m33 = m33;
        return this;
    }

    public @NotNull Vec translation() {
        return new Vec(m30, m31, m32);
    }

    public @NotNull SVD svdDecompose() {
        return SVD.decompose(this);
    }

    @Override
    public String toString() {
        return "Mat4{" +
                m00 +
                ", " + m01 +
                ", " + m02 +
                ", " + m03 +
                "\n" + m10 +
                ", " + m11 +
                ", " + m12 +
                ", " + m13 +
                "\n" + m20 +
                ", " + m21 +
                ", " + m22 +
                ", " + m23 +
                "\n" + m30 +
                ", " + m31 +
                ", " + m32 +
                ", " + m33 +
                '}';
    }
}
