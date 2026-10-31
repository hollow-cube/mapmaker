package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Apply writes below `setBlock`, so it must drop the cached sky occlusion itself or a later relight seeds sky
/// light from the pre-edit terrain.
@EnvTest
class SkyOcclusionTest {

    @Test
    void relightSeesSkyOcclusionChangedByApply(Env env) {
        var instance = (InstanceContainer) env.createEmptyInstance();
        instance.setChunkSupplier(LightingChunk::new);
        instance.loadChunk(0, 0).join();
        Chunk chunk = instance.getChunk(0, 0);

        ApplyEngine.DEFAULT.apply(instance, roof(Block.STONE));
        LightingChunk.relight(instance, List.of(chunk));
        assertEquals(0, skyLight(instance, 8, 65, 8), "the roof shades y=65");

        ApplyEngine.DEFAULT.apply(instance, roof(Block.AIR));
        LightingChunk.relight(instance, List.of(chunk));
        assertEquals(15, skyLight(instance, 8, 65, 8), "removing the roof reopens the sky");
    }

    private static EditBuffer roof(Block block) {
        var builder = EditBuffer.builder();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) builder.setBlockState(x, 70, z, block);
        }
        return builder.build();
    }

    private static int skyLight(Instance instance, int x, int y, int z) {
        var section = instance.getChunk(x >> 4, z >> 4).getSection(y >> 4);
        return section.skyLight().getLevel(x & 15, y & 15, z & 15);
    }
}
