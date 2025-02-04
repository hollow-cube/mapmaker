package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.terraform.instance.TerraformBiomeChunk;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LitChunk extends LightingChunk implements ChunkExt {
    private final Heightmaps heightmaps;

    public LitChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ);
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
    public @NotNull DynamicRegistry.Key<Biome> getBiome(int x, int y, int z) {
        var key = TerraformBiomeChunk.getBiome(this, x, y, z);
        return key != null ? key : super.getBiome(x, y, z);
    }

    @Override
    public void setBiome(int x, int y, int z, @NotNull DynamicRegistry.Key<Biome> biome) {
        if (!TerraformBiomeChunk.setBiome(this, x, y, z, biome)) {
            super.setBiome(x, y, z, biome);
        }
    }
}
