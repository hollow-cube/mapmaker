package net.hollowcube.mapmaker.map.instance;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

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

    @Override
    protected NBTCompound computeHeightmap() {
        return heightmaps.getProtocolData();
    }
}
