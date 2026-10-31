package net.hollowcube.terraform.util;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransformTest {

    @Test
    void identityIsIdentity() {
        assertTrue(Transform.identity().isIdentity());
        assertFalse(Transform.rotateY(90).isIdentity());
        assertFalse(Transform.translate(1, 0, 0).isIdentity());
    }

    @Test
    void linearIdentityIgnoresTranslation() {
        assertTrue(Transform.translate(4, 5, 6).isLinearIdentity());
        assertFalse(Transform.translate(4, 5, 6).compose(Transform.rotateY(90)).isLinearIdentity());
        assertFalse(Transform.mirror(Transform.Axis.Y).isLinearIdentity());
    }

    @Test
    void mirrorTwiceIsIdentity() {
        for (Transform.Axis axis : Transform.Axis.values()) {
            Transform m = Transform.mirror(axis);
            assertTrue(m.compose(m).isIdentity(), "mirror " + axis + " twice");
        }
    }

    @Test
    void rotateFourTimesIsIdentity() {
        for (Transform r : List.of(
            Transform.rotateX(90),
            Transform.rotateY(90),
            Transform.rotateZ(90)
        )) {
            assertTrue(r.compose(r).compose(r).compose(r).isIdentity(), "4 x 90 of " + r);
        }
    }

    @Test
    void rotateThenRotateBackIsIdentity() {
        assertTrue(Transform.rotateY(90).compose(Transform.rotateY(-90)).isIdentity());
        assertNear(Transform.identity(), Transform.rotateY(37).compose(Transform.rotateY(-37)));
    }

    @Test
    void negativeAndWrappedAnglesNormalize() {
        assertNear(Transform.rotateY(270), Transform.rotateY(-90));
        assertNear(Transform.rotateY(90), Transform.rotateY(450));
        assertTrue(Transform.rotateY(360).isIdentity());
    }

    @Test
    void composeIsAssociative() {
        Transform a = Transform.rotateY(45);
        Transform b = Transform.mirror(Transform.Axis.Z);
        Transform c = Transform.translate(1, 2, 3);
        assertNear(a.compose(b).compose(c), a.compose(b.compose(c)));
    }

    @Test
    void composeMeansFirstThenThis() {
        Transform rotate = Transform.rotateY(90), shift = Transform.translate(1, 0, 0);
        // Shifted to (2, 0, 0), then turned onto +z.
        assertEquals(new BlockVec(-1, 0, 2), rotate.compose(shift).applyBlock(1, 0, 0));
        // Turned to (-1, 0, 1), then shifted.
        assertEquals(new BlockVec(0, 0, 1), shift.compose(rotate).applyBlock(1, 0, 0));
    }

    @Test
    void applyDirectionIgnoresTranslation() {
        Vec v = Transform.translate(10, 20, 30)
            .compose(Transform.rotateY(90))
            .applyDirection(1, 0, 0);
        assertNear(0, v.x());
        assertNear(0, v.y());
        assertNear(1, v.z());
    }

    @Test
    void cubeSymmetryClassifier() {
        assertTrue(Transform.identity().isCubeSymmetry());
        assertTrue(Transform.rotateY(90).isCubeSymmetry());
        assertTrue(Transform.rotateY(180).isCubeSymmetry());
        assertTrue(Transform.mirror(Transform.Axis.X).isCubeSymmetry());
        assertTrue(Transform.rotateX(90).compose(Transform.rotateY(90)).isCubeSymmetry());
        assertTrue(Transform.translate(10, -3, 7).isCubeSymmetry());

        assertFalse(Transform.rotateY(45).isCubeSymmetry());
        assertFalse(new Transform(2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0).isCubeSymmetry(), "scale");
        assertFalse(new Transform(1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0).isCubeSymmetry(), "shear");
        assertFalse(new Transform(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0).isCubeSymmetry(), "collapse");
    }

    @Test
    void flipClassifiers() {
        assertTrue(Transform.mirror(Transform.Axis.X).isHorizontalFlip());
        assertTrue(Transform.mirror(Transform.Axis.Z).isHorizontalFlip());
        assertFalse(Transform.mirror(Transform.Axis.Y).isHorizontalFlip());
        assertFalse(Transform.rotateY(90).isHorizontalFlip(), "a rotation is not a flip");
        assertFalse(
            Transform.mirror(Transform.Axis.X)
                .compose(Transform.mirror(Transform.Axis.Z))
                .isHorizontalFlip(),
            "two horizontal flips are a half turn"
        );

        assertTrue(Transform.mirror(Transform.Axis.Y).isVerticalFlip());
        assertFalse(Transform.mirror(Transform.Axis.X).isVerticalFlip());
        assertFalse(Transform.rotateY(90).isVerticalFlip());
    }

    @Test
    void applyBlockTranslate() {
        assertEquals(new BlockVec(6, 2, 3), Transform.translate(5, 0, 0).applyBlock(1, 2, 3));
    }

    @Test
    void applyBlockRotateMapsCellCenter() {
        // Centre (1.5, 0.5, 0.5) turns to (-0.5, 0.5, 1.5).
        assertEquals(new BlockVec(-1, 0, 1), Transform.rotateY(90).applyBlock(1, 0, 0));
    }

    @Test
    void applyBlockIsALatticeBijectionForEveryCubeSymmetry() {
        List<Transform> symmetries = List.of(
            Transform.rotateY(90),
            Transform.rotateY(270),
            Transform.rotateX(90),
            Transform.rotateZ(180),
            Transform.mirror(Transform.Axis.X),
            Transform.mirror(Transform.Axis.Y).compose(Transform.rotateY(90)),
            Transform.translate(-3, 7, 2).compose(Transform.rotateX(90))
        );
        for (Transform t : symmetries) {
            assertTrue(t.isCubeSymmetry());
            Set<BlockVec> images = new HashSet<>();
            for (int x = -2; x < 4; x++)
                for (int y = 0; y < 5; y++)
                    for (int z = -1; z < 3; z++) images.add(t.applyBlock(x, y, z));
            assertEquals(6 * 5 * 4, images.size(), "no two cells collide under " + t);
        }
    }

    @Test
    void ninetyDegreeRotationsAreExactSignedPermutations() {
        assertNear(new Transform(0, 0, -1, 0, 0, 1, 0, 0, 1, 0, 0, 0), Transform.rotateY(90));
        assertNear(new Transform(1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0), Transform.rotateX(90));
        assertNear(new Transform(0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0), Transform.rotateZ(90));
        for (int deg = -360; deg <= 720; deg += 90) {
            for (Transform t : List.of(
                Transform.rotateX(deg),
                Transform.rotateY(deg),
                Transform.rotateZ(deg)
            )) {
                for (double v : linear(t)) {
                    assertTrue(v == 0 || Math.abs(v) == 1, "exact coefficient " + v + " in " + t);
                }
            }
        }
    }

    private static void assertNear(double expected, double actual) {
        assertTrue(Math.abs(expected - actual) < 1e-9, "expected ~" + expected + " got " + actual);
    }

    private static void assertNear(Transform a, Transform b) {
        double[] x = coefficients(a), y = coefficients(b);
        for (int i = 0; i < 12; i++) assertNear(x[i], y[i]);
    }

    private static double[] linear(Transform t) {
        return new double[] {
            t.m00(),
            t.m01(),
            t.m02(),
            t.m10(),
            t.m11(),
            t.m12(),
            t.m20(),
            t.m21(),
            t.m22(),
        };
    }

    private static double[] coefficients(Transform t) {
        return new double[] {
            t.m00(),
            t.m01(),
            t.m02(),
            t.m03(),
            t.m10(),
            t.m11(),
            t.m12(),
            t.m13(),
            t.m20(),
            t.m21(),
            t.m22(),
            t.m23(),
        };
    }
}
