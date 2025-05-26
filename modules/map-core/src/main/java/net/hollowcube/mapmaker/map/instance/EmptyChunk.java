package net.hollowcube.mapmaker.map.instance;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.heightmap.Heightmap;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.UpdateLightPacket;
import net.minestom.server.network.packet.server.play.data.ChunkData;
import net.minestom.server.network.packet.server.play.data.LightData;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.SHORT;

public class EmptyChunk extends Chunk {
    private static final List<Section> SECTIONS;

    static {
        var theSections = new ArrayList<Section>();
        for (int i = 0; i < 24; i++) {
            theSections.add(new Section());
        }
        SECTIONS = List.copyOf(theSections);
    }

    public EmptyChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ, false);
    }

    @Override
    protected void setBlock(int x, int y, int z, @NotNull Block block, BlockHandler.@Nullable Placement placement, BlockHandler.@Nullable Destroy destroy) {

    }

    @Override
    public @NotNull List<Section> getSections() {
        return SECTIONS;
    }

    @Override
    public @NotNull Section getSection(int section) {
        return SECTIONS.get(section - minSection);
    }

    @Override
    public @NotNull Heightmap motionBlockingHeightmap() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public @NotNull Heightmap worldSurfaceHeightmap() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void loadHeightmapsFromNBT(CompoundBinaryTag heightmaps) {

    }

    @Override
    public void tick(long time) {

    }

    @Override
    public long getLastChangeTime() {
        return 0;
    }

    @Override
    public @NotNull SendablePacket getFullDataPacket() {
        return createChunkPacket();
    }


    private @NotNull ChunkDataPacket createChunkPacket() {
        final byte[] data;
        data = NetworkBuffer.makeArray(networkBuffer -> {
            for (Section section : SECTIONS) {
                networkBuffer.write(SHORT, (short) section.blockPalette().count());
                networkBuffer.write(Palette.BLOCK_SERIALIZER, section.blockPalette());
                networkBuffer.write(Palette.BIOME_SERIALIZER, section.biomePalette());
            }
        });

        return new ChunkDataPacket(chunkX, chunkZ,
                new ChunkData(Map.of(), data, Map.of()),
                createLightData(true)
        );
    }

    @NotNull UpdateLightPacket createLightPacket() {
        return new UpdateLightPacket(chunkX, chunkZ, createLightData(false));
    }

    protected LightData createLightData(boolean requiredFullChunk) {
        BitSet skyMask = new BitSet();
        BitSet blockMask = new BitSet();
        BitSet emptySkyMask = new BitSet();
        BitSet emptyBlockMask = new BitSet();
        List<byte[]> skyLights = new ArrayList<>();
        List<byte[]> blockLights = new ArrayList<>();

        int index = 0;
        for (Section section : SECTIONS) {
            index++;
            final byte[] skyLight = section.skyLight().array();
            final byte[] blockLight = section.blockLight().array();
            if (skyLight.length != 0) {
                skyLights.add(skyLight);
                skyMask.set(index);
            } else {
                emptySkyMask.set(index);
            }
            if (blockLight.length != 0) {
                blockLights.add(blockLight);
                blockMask.set(index);
            } else {
                emptyBlockMask.set(index);
            }
        }
        return new LightData(
                skyMask, blockMask,
                emptySkyMask, emptyBlockMask,
                skyLights, blockLights
        );
    }

    @Override
    public @NotNull Chunk copy(@NotNull Instance instance, int chunkX, int chunkZ) {
        return new EmptyChunk(instance, chunkX, chunkZ);
    }

    @Override
    public void reset() {

    }

    @Override
    public void invalidate() {

    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return Block.AIR;
    }

    @Override
    public DynamicRegistry.@NotNull Key<Biome> getBiome(int x, int y, int z) {
        return Biome.PLAINS;
    }

    @Override
    public void setBiome(int x, int y, int z, DynamicRegistry.@NotNull Key<Biome> biome) {

    }
}
