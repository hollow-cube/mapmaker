package net.hollowcube.aj.entity;

import net.hollowcube.aj.model.ModelKeyframe;
import net.hollowcube.aj.model.ModelNode;
import net.hollowcube.mql.MqlModule;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;
import java.util.List;
import java.util.Map;

public class AnimEntityV2 extends Entity {
    private final ModelNode.BaseProps base;

    public AnimEntityV2(@NotNull EntityType entityType, @NotNull ModelNode.BaseProps base) {
        super(entityType);
        this.base = base;

        hasPhysics = false;
        setNoGravity(true);
        hasCollision = false;
        setSynchronizationTicks(Long.MAX_VALUE);

        final var meta = getEntityMeta();
        var transform = base.defaultTransform().decomposed();
        meta.setTranslation(new Vec(transform.translation()[0], transform.translation()[1], transform.translation()[2]));
        meta.setLeftRotation(transform.leftRotation());
        meta.setScale(new Vec(transform.scale()[0], transform.scale()[1], transform.scale()[2]));
    }

    @Override
    public @NotNull AbstractDisplayMeta getEntityMeta() {
        return (AbstractDisplayMeta) super.getEntityMeta();
    }

    public void applyFrame(
            double duration,
            @NotNull MqlModule.Instance scriptInstance,
            @NotNull AnimQuery scriptQuery,
            @NotNull List<ModelKeyframe> keyframes
    ) {
        for (var keyframe : keyframes) {
            switch (keyframe) {
                case ModelKeyframe.Vec3 vec3 -> {
                    var meta = getEntityMeta();
                    var newValue = new Vec(
                            scriptInstance.getScript(vec3.value().x()).eval(scriptQuery),
                            scriptInstance.getScript(vec3.value().y()).eval(scriptQuery),
                            scriptInstance.getScript(vec3.value().z()).eval(scriptQuery)
                    );

                    meta.setNotifyAboutChanges(false);
                    meta.setTransformationInterpolationDuration(1);
                    meta.setTransformationInterpolationStartDelta(0);

                    switch (vec3.channel()) {
                        case POSITION -> {
                            System.out.println("Setting position to " + newValue);
                            var tr = base.defaultTransform().decomposed().translation();
                            var pos = new Vec(tr[0], tr[1], tr[2]);
                            meta.setTranslation(pos.add(newValue.div(16)));
                        }
                        case ROTATION -> {
                            // yxz

//                            meta.setLeftRotation(new float[]{0, 0, 0, 0});
                            meta.setLeftRotation(eulerToQuaternion(newValue));

//                            float yaw = (float) Math.toRadians(newValue.y());
//                            float pitch = (float) Math.toRadians(newValue.x());
//                            float roll = (float) Math.toRadians(newValue.z());
//
//                            var fs = new float[16];
//                            for (int i = 0; i < 16; i++) {
//                                fs[i] = base.defaultTransform().matrix().get(i);
//                            }
//                            var mat = new Matrix4d().set(fs)
//                                    .rotateY(yaw).rotateX(pitch).rotateZ(roll);
//
//                            var q = new Quaternionf().setFromNormalized(mat);
//
//                            System.out.println("Setting rotation to " + newValue);
//                            meta.setLeftRotation(new float[]{q.x, q.y, q.z, q.w});
                        }
                        case SCALE -> {
                            meta.setScale(newValue);
                        }
                    }
                    meta.setNotifyAboutChanges(true);
                }
                case ModelKeyframe.Variant variant -> {
                }
                case ModelKeyframe.Commands commands -> {
                }
            }
        }

    }

    public static float[] eulerToQuaternion(Vec v) {


        var p = new Vec(
                Math.toRadians(v.y()),
                Math.toRadians(v.z()),
                Math.toRadians(v.x())
        );

        double cy = Math.cos(p.z() * 0.5);
        double sy = Math.sin(p.z() * 0.5);
        double cp = Math.cos(p.y() * 0.5);
        double sp = Math.sin(p.y() * 0.5);
        double cr = Math.cos(p.x() * 0.5);
        double sr = Math.sin(p.x() * 0.5);

        return new float[]{
                (float) (cr * cp * cy + sr * sp * sy),
                (float) (sr * cp * cy - cr * sp * sy),
                (float) (cr * sp * cy + sr * cp * sy),
                (float) (cr * cp * sy - sr * sp * cy)
        };
    }

    // Convert to rotation matrix
//        Matrix3f rotationMatrix = new Matrix3f().rotateY(yaw).rotateX(pitch).rotateZ(roll);
//
//        // Convert rotation matrix to quaternion
//        Quaternionf quaternion = new Quaternionf().setFromNormalized(rotationMatrix);
//        return new float[]{quaternion.x, quaternion.y, quaternion.z, quaternion.w};

//        float yaw = (float) Math.toRadians(v.y());    // Z-axis rotation (in degrees)
//        float pitch = (float) Math.toRadians(v.x());  // Y-axis rotation (in degrees)
//        float roll = (float) Math.toRadians(v.z());   // X-axis rotation (in degrees)
//
//        var qx = Math.sin(roll / 2) * Math.cos(pitch / 2) * Math.cos(yaw / 2) - Math.cos(roll / 2) * Math.sin(pitch / 2) * Math.sin(yaw / 2);
//        var qy = Math.cos(roll / 2) * Math.sin(pitch / 2) * Math.cos(yaw / 2) + Math.sin(roll / 2) * Math.cos(pitch / 2) * Math.sin(yaw / 2);
//        var qz = Math.cos(roll / 2) * Math.cos(pitch / 2) * Math.sin(yaw / 2) - Math.sin(roll / 2) * Math.sin(pitch / 2) * Math.cos(yaw / 2);
//        var qw = Math.cos(roll / 2) * Math.cos(pitch / 2) * Math.cos(yaw / 2) + Math.sin(roll / 2) * Math.sin(pitch / 2) * Math.sin(yaw / 2);
//        return new float[]{(float) qx, (float) qy, (float) qz, (float) qw};

    // Compute the quaternion components
//        float cy = (float) Math.cos(yaw * 0.5);
//        float sy = (float) Math.sin(yaw * 0.5);
//        float cp = (float) Math.cos(pitch * 0.5);
//        float sp = (float) Math.sin(pitch * 0.5);
//        float cr = (float) Math.cos(roll * 0.5);
//        float sr = (float) Math.sin(roll * 0.5);
//
//        float qw = cr * cp * cy + sr * sp * sy;
//        float qx = sr * cp * cy - cr * sp * sy;
//        float qy = cr * sp * cy + sr * cp * sy;
//        float qz = cr * cp * sy - sr * sp * cy;
//
//        return new float[]{qx, qy, qz, qw};


    public static Map.Entry<Quaternionf, Quaternionf> svdDecompose(Matrix3f $$0) {
        GivensParameters $$12;
        Matrix3f $$1 = new Matrix3f((Matrix3fc) $$0);
        $$1.transpose();
        $$1.mul((Matrix3fc) $$0);
        Quaternionf $$2 = eigenvalueJacobi($$1, 5);
        float $$3 = $$1.m00;
        float $$4 = $$1.m11;
        boolean $$5 = (double) $$3 < 1.0E-6;
        boolean $$6 = (double) $$4 < 1.0E-6;
        Matrix3f $$7 = $$1;
        Matrix3f $$8 = $$0.rotate((Quaternionfc) $$2);
        Quaternionf $$9 = new Quaternionf();
        Quaternionf $$10 = new Quaternionf();
        if ($$5) {
            $$12 = qrGivensQuat($$8.m11, -$$8.m10);
        } else {
            $$12 = qrGivensQuat($$8.m00, $$8.m01);
        }
        Quaternionf $$13 = $$12.aroundZ($$10);
        Matrix3f $$14 = $$12.aroundZ($$7);
        $$9.mul((Quaternionfc) $$13);
        $$14.transpose().mul((Matrix3fc) $$8);
        $$7 = $$8;
        $$12 = $$5 ? qrGivensQuat($$14.m22, -$$14.m20) : qrGivensQuat($$14.m00, $$14.m02);
        $$12 = $$12.inverse();
        Quaternionf $$15 = $$12.aroundY($$10);
        Matrix3f $$16 = $$12.aroundY($$7);
        $$9.mul((Quaternionfc) $$15);
        $$16.transpose().mul((Matrix3fc) $$14);
        $$7 = $$14;
        $$12 = $$6 ? qrGivensQuat($$16.m22, -$$16.m21) : qrGivensQuat($$16.m11, $$16.m12);
        Quaternionf $$17 = $$12.aroundX($$10);
        Matrix3f $$18 = $$12.aroundX($$7);
        $$9.mul((Quaternionfc) $$17);
        $$18.transpose().mul((Matrix3fc) $$16);
        Vector3f $$19 = new Vector3f($$18.m00, $$18.m11, $$18.m22);
        return Map.entry($$9, $$2.conjugate());
    }

    private static GivensParameters qrGivensQuat(float $$0, float $$1) {
        float $$2 = (float) java.lang.Math.hypot($$0, $$1);
        float $$3 = $$2 > 1.0E-6f ? $$1 : 0.0f;
        float $$4 = Math.abs((float) $$0) + Math.max((float) $$2, (float) 1.0E-6f);
        if ($$0 < 0.0f) {
            float $$5 = $$3;
            $$3 = $$4;
            $$4 = $$5;
        }
        return GivensParameters.fromUnnormalized($$3, $$4);
    }

    public record GivensParameters(float sinHalf, float cosHalf) {


        public static GivensParameters fromPositiveAngle(float $$0) {
            float $$1 = org.joml.Math.sin((float) ($$0 / 2.0f));
            float $$2 = org.joml.Math.cosFromSin((float) $$1, (float) ($$0 / 2.0f));
            return new GivensParameters($$1, $$2);
        }

        public static GivensParameters fromUnnormalized(float $$0, float $$1) {
            float $$2 = org.joml.Math.invsqrt((float) ($$0 * $$0 + $$1 * $$1));
            return new GivensParameters($$2 * $$0, $$2 * $$1);
        }

        public GivensParameters inverse() {
            return new GivensParameters(-this.sinHalf, this.cosHalf);
        }

        public Quaternionf aroundX(Quaternionf $$0) {
            return $$0.set(this.sinHalf, 0.0f, 0.0f, this.cosHalf);
        }

        public Quaternionf aroundY(Quaternionf $$0) {
            return $$0.set(0.0f, this.sinHalf, 0.0f, this.cosHalf);
        }

        public Quaternionf aroundZ(Quaternionf $$0) {
            return $$0.set(0.0f, 0.0f, this.sinHalf, this.cosHalf);
        }

        public float cos() {
            return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
        }

        public float sin() {
            return 2.0f * this.sinHalf * this.cosHalf;
        }

        public Matrix3f aroundX(Matrix3f $$0) {
            $$0.m01 = 0.0f;
            $$0.m02 = 0.0f;
            $$0.m10 = 0.0f;
            $$0.m20 = 0.0f;
            float $$1 = this.cos();
            float $$2 = this.sin();
            $$0.m11 = $$1;
            $$0.m22 = $$1;
            $$0.m12 = $$2;
            $$0.m21 = -$$2;
            $$0.m00 = 1.0f;
            return $$0;
        }

        public Matrix3f aroundY(Matrix3f $$0) {
            $$0.m01 = 0.0f;
            $$0.m10 = 0.0f;
            $$0.m12 = 0.0f;
            $$0.m21 = 0.0f;
            float $$1 = this.cos();
            float $$2 = this.sin();
            $$0.m00 = $$1;
            $$0.m22 = $$1;
            $$0.m02 = -$$2;
            $$0.m20 = $$2;
            $$0.m11 = 1.0f;
            return $$0;
        }

        public Matrix3f aroundZ(Matrix3f $$0) {
            $$0.m02 = 0.0f;
            $$0.m12 = 0.0f;
            $$0.m20 = 0.0f;
            $$0.m21 = 0.0f;
            float $$1 = this.cos();
            float $$2 = this.sin();
            $$0.m00 = $$1;
            $$0.m11 = $$1;
            $$0.m01 = $$2;
            $$0.m10 = -$$2;
            $$0.m22 = 1.0f;
            return $$0;
        }
    }


    private static void similarityTransform(Matrix3f $$0, Matrix3f $$1) {
        $$0.mul((Matrix3fc) $$1);
        $$1.transpose();
        $$1.mul((Matrix3fc) $$0);
        $$0.set((Matrix3fc) $$1);
    }

    private static final float G = 3.0f + 2.0f * org.joml.Math.sqrt((float) 2.0f);
    private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle(0.7853982f);

    private static GivensParameters approxGivensQuat(float $$0, float $$1, float $$2) {
        float $$4 = $$1;
        float $$3 = 2.0f * ($$0 - $$2);
        if (G * $$4 * $$4 < $$3 * $$3) {
            return GivensParameters.fromUnnormalized($$4, $$3);
        }
        return PI_4;
    }

    private static void stepJacobi(Matrix3f $$0, Matrix3f $$1, Quaternionf $$2, Quaternionf $$3) {
        if ($$0.m01 * $$0.m01 + $$0.m10 * $$0.m10 > 1.0E-6f) {
            GivensParameters $$4 = approxGivensQuat($$0.m00, 0.5f * ($$0.m01 + $$0.m10), $$0.m11);
            Quaternionf $$5 = $$4.aroundZ($$2);
            $$3.mul((Quaternionfc) $$5);
            $$4.aroundZ($$1);
            similarityTransform($$0, $$1);
        }
        if ($$0.m02 * $$0.m02 + $$0.m20 * $$0.m20 > 1.0E-6f) {
            GivensParameters $$6 = approxGivensQuat($$0.m00, 0.5f * ($$0.m02 + $$0.m20), $$0.m22).inverse();
            Quaternionf $$7 = $$6.aroundY($$2);
            $$3.mul((Quaternionfc) $$7);
            $$6.aroundY($$1);
            similarityTransform($$0, $$1);
        }
        if ($$0.m12 * $$0.m12 + $$0.m21 * $$0.m21 > 1.0E-6f) {
            GivensParameters $$8 = approxGivensQuat($$0.m11, 0.5f * ($$0.m12 + $$0.m21), $$0.m22);
            Quaternionf $$9 = $$8.aroundX($$2);
            $$3.mul((Quaternionfc) $$9);
            $$8.aroundX($$1);
            similarityTransform($$0, $$1);
        }
    }

    public static Quaternionf eigenvalueJacobi(Matrix3f $$0, int $$1) {
        Quaternionf $$2 = new Quaternionf();
        Matrix3f $$3 = new Matrix3f();
        Quaternionf $$4 = new Quaternionf();
        for (int $$5 = 0; $$5 < $$1; ++$$5) {
            stepJacobi($$0, $$3, $$4, $$2);
        }
        $$2.normalize();
        return $$2;
    }

    public static float[] eulerToSVDLeftQuaternion(Vec v) {
        // Convert Euler angles to quaternion
        float[] quaternion = eulerToQuaternion(v);

        // Normalize the quaternion to ensure it represents a rotation
        float norm = (float) Math.sqrt(quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] +
                quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3]);

        for (int i = 0; i < 4; i++) {
            quaternion[i] /= norm;
        }

        return quaternion;
    }

}
