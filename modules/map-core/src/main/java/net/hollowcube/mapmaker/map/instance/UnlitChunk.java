package net.hollowcube.mapmaker.map.instance;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.data.LightData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;

public class UnlitChunk extends DynamicChunk implements ChunkExt {

    private final Heightmaps heightmaps;
    private final LightData light;

    public UnlitChunk(@NotNull Instance instance, int chunkX, int chunkZ, LightData light) {
        super(instance, chunkX, chunkZ);
        this.light = light;
        this.heightmaps = new Heightmaps(this);
    }

    @Override
    public int getHeight(int heightmap, int x, int z) {
        return heightmaps.get(heightmap, x, z);
    }

    @Override
    public @NotNull Heightmap heightmap(int heightmap) {
        return Objects.requireNonNull(heightmaps.heightmap(heightmap), "no such heightmap: " + heightmap);
    }

    @Override
    public void loadHeightmap(int heightmap, int[] data) {
        heightmaps.load(heightmap, data);
    }

    @Override
    public int[] saveHeightmap(int heightmap) {
        return heightmaps.save(heightmap);
    }

    @Override
    public void setBlock(
            int x, int y, int z, @NotNull Block block,
            @Nullable BlockHandler.Placement placement,
            @Nullable BlockHandler.Destroy destroy
    ) {
        super.setBlock(x, y, z, block, placement, destroy);
        heightmaps.update(x, y, z, block);
    }

    public Heightmaps heightmaps() {
        return heightmaps;
    }

    @Override
    protected CompoundBinaryTag getHeightmapNBT() {
        return heightmaps.getProtocolData();
    }

    @Override
    protected LightData createLightData(boolean requiredFullChunk) {
        return this.light;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static LightData createStaticLightData(
            @NotNull Instance instance,
            @Range(from = 0, to = 15) int skyLight,
            @Range(from = 0, to = 15) int blockLight
    ) {
        int sectionCount = instance.getCachedDimensionType().height() / 16;

        BitSet full = new BitSet();
        full.set(0, sectionCount);

        List<byte[]> sky;
        List<byte[]> block;

        if (skyLight != 0) {
            byte[] skyData = new byte[2048];
            Arrays.fill(skyData, (byte) (skyLight | (skyLight << 4)));
            sky = Collections.nCopies(sectionCount, skyData);
        } else {
            sky = List.of();
        }

        if (blockLight != 0) {
            byte[] blockData = new byte[2048];
            Arrays.fill(blockData, (byte) (blockLight | (blockLight << 4)));
            block = Collections.nCopies(sectionCount, blockData);
        } else {
            block = List.of();
        }

        return new LightData(
                skyLight == 0 ? new BitSet() : full,
                blockLight == 0 ? new BitSet() : full,
                skyLight != 0 ? new BitSet() : full,
                blockLight != 0 ? new BitSet() : full,
                sky,
                block
        );
    }
}
