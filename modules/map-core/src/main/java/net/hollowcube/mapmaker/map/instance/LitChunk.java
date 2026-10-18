package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.terraform.instance.TerraformBiomeChunk;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class LitChunk extends LightingChunk implements ChunkExt {
    private final Heightmaps heightmaps;

    public LitChunk(Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ);
        this.heightmaps = new Heightmaps(this);
    }

    @Override
    public int getHeight(int heightmap, int x, int z) {
        return heightmaps.get(heightmap, x, z);
    }

    @Override
    public Heightmap heightmap(int heightmap) {
        return Objects.requireNonNull(heightmaps.heightmap(heightmap), "no such heightmap: " + heightmap);
    }

    @Override
    public void loadHeightmap(int heightmap, int @Nullable [] data) {
        heightmaps.load(heightmap, data);
    }

    @Override
    public int @Nullable [] saveHeightmap(int heightmap) {
        return heightmaps.save(heightmap);
    }

    @Override
    public void setBlock(
            int x, int y, int z, Block block,
            @Nullable BlockHandler.Placement placement,
            @Nullable BlockHandler.Destroy destroy
    ) {
        var previous = entries.get(CoordConversion.chunkBlockIndex(x, y, z));
        super.setBlock(x, y, z, block, placement, destroy);
        heightmaps.update(x, y, z, block);
        WorldIndex.blockChanged(instance, x, y, z, previous, block);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        WorldIndex.chunkLoaded(this, entries);
    }

    public Heightmaps heightmaps() {
        return heightmaps;
    }

    @Override
    protected Map<net.minestom.server.instance.heightmap.Heightmap.Type, long[]> getHeightmaps() {
        return heightmaps.getProtocolData();
    }

    @Override
    public RegistryKey<Biome> getBiome(int x, int y, int z) {
        var key = TerraformBiomeChunk.getBiome(this, x, y, z);
        return key != null ? key : super.getBiome(x, y, z);
    }

    @Override
    public void setBiome(int x, int y, int z, RegistryKey<Biome> biome) {
        if (!TerraformBiomeChunk.setBiome(this, x, y, z, biome)) {
            super.setBiome(x, y, z, biome);
        }
    }
}
