package net.hollowcube.mapmaker.map.instance;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.data.LightData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
}
