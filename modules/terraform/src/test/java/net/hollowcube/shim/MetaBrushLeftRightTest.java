package net.hollowcube.shim;

import com.jayemceekay.metabrushes.common.brushes.*;
import com.sk89q.worldedit.EditSession;
import com.thevoxelbox.voxelsniper.brush.type.AbstractBrush;
import com.thevoxelbox.voxelsniper.sniper.Sniper;
import com.thevoxelbox.voxelsniper.sniper.snipe.Snipe;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
class MetaBrushLeftRightTest {

    static {
        System.setProperty("minestom.inside-test", "true");
    }

    @Test
    void testAirosion(Env env) {
        execBrushTest(env, new AirosionBrush());
    }

    @Test
    void testAngledCliff(Env env) {
        execBrushTest(env, new AngledCliffBrush());
    }

    @Test
    void testBlendEllipsoid(Env env) {
        execBrushTest(env, new BlendEllipsoidBrush());
    }

    @Test
    void testCavernGeneration(Env env) {
        execBrushTest(env, new CavernGenerationBrush());
    }

    //todo seems like it needs water
//    @Test
//    void testCoastalErosion(Env env) {
//        execBrushTest(env, new CoastalErosionBrush());
//    }

    @Test
    void testConvolutionSurfaceTest(Env env) {
        execBrushTest(env, new ConvolutionSurfaceTestBrush());
    }

    @Test
    void testCrackBrush(Env env) {
        execBrushTest(env, new CrackBrush());
    }

    @Test
    void testDirectionalOverlay(Env env) {
        execBrushTest(env, new DirectionalOverlayBrush());
    }

    @Test
    void testEnhancedSpline(Env env) {
        execBrushTest(env, new EnhancedSplineBrush());
    }

    @Test
    void testEscarpment(Env env) {
        execBrushTest(env, new EscarpmentBrush());
    }

    @Test
    void testExpandilate(Env env) {
        execBrushTest(env, new ExpandilateBrush());
    }

    @Test
    void testMaximaHydrosion(Env env) {
        execBrushTest(env, new MaximaHydrosionBrush());
    }

    @Test
    void testMetaBalls(Env env) {
        execBrushTest(env, new MetaBallsBrush());
    }

    @Test
    void testMetaBlob(Env env) {
        execBrushTest(env, new MetaBlobBrush());
    }

    @Test
    void testMetaCylinders(Env env) {
        execBrushTest(env, new MetaCylindersBrush());
    }

    @Test
    void testMetaErodeBlend(Env env) {
        execBrushTest(env, new MetaErodeBlendBrush());
    }

    @Test
    void testMetaErode(Env env) {
        execBrushTest(env, new MetaErodeBrush());
    }

    @Test
    void testMetaSnow(Env env) {
        execBrushTest(env, new MetaSnowBrush());
    }

    @Test
    void testMetaSplines(Env env) {
        execBrushTest(env, new MetaSplinesBrush());
    }

    @Test
    void testMetaVoxels(Env env) {
        execBrushTest(env, new MetaVoxelsBrush());
    }

    @Test
    void testRandomHydrosion(Env env) {
        execBrushTest(env, new RandomHydrosionBrush());
    }

    @Test
    void testSpreadHydrosion(Env env) {
        execBrushTest(env, new SpreadHydrosionBrush());
    }

    @Test
    void testStalacmite(Env env) {
        execBrushTest(env, new StalacmiteBrush());
    }

    @Test
    void testTestSmoothing(Env env) {
        execBrushTest(env, new TestSmoothingBrush());
    }

    @Test
    void testWaterSmoothing(Env env) {
        execBrushTest(env, new WaterSmoothingBrush());
    }

    private void execBrushTest(Env env, AbstractBrush brush) {
        var instance = env.createFlatInstance();
        var player = env.createPlayer(instance, new Pos(0, 41, 0, 62, 39));

        brush.loadProperties();
        brush.wrappedHandleArrowAction(new Snipe(
                new EditSession(instance),
                new Sniper(player)
        ));
        brush.wrappedHandleGunpowderAction(new Snipe(
                new EditSession(instance),
                new Sniper(player)
        ));
    }
}
