package net.hollowcube.mapmaker.map.instance;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
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
    public @NotNull Chunk copy(@NotNull Instance instance, int chunkX, int chunkZ) {
        LitChunk lightingChunk = new LitChunk(instance, chunkX, chunkZ);
        lightingChunk.sections = sections.stream().map(Section::clone).toList();
        lightingChunk.entries.putAll(entries);
        lightingChunk.tickableMap.putAll(tickableMap);
        for (int i = 0; i < 3; i++) {
            lightingChunk.heightmaps.load(i, heightmaps.save(i));
        }
        copyPartialLightData(lightingChunk);
        copyFullLightData(lightingChunk);
        lightingChunk.onLoad(); // Mark as ready
        return lightingChunk;
    }

    private void copyPartialLightData(@NotNull LitChunk target) {
        class Holder {
            static Field field;
//            static MethodHandle getter;
//            static MethodHandle setter;
        }
        try {
            if (Holder.field == null) {
                Holder.field = LightingChunk.class.getDeclaredField("partialLightData");
                Holder.field.setAccessible(true);
//                Holder.getter = MethodHandles.lookup().unreflectGetter(field);
//                Holder.setter = MethodHandles.lookup().unreflectSetter(field);
            }

            Holder.field.set(target, Holder.field.get(this));
//            Holder.setter.invoke(target, Holder.getter.invoke(this));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void copyFullLightData(@NotNull LitChunk target) {
        class Holder {
            static Field field;
//            static MethodHandle getter;
//            static MethodHandle setter;
        }
        try {
            if (Holder.field == null) {
                Holder.field = LightingChunk.class.getDeclaredField("fullLightData");
                Holder.field.setAccessible(true);
//                Holder.getter = MethodHandles.lookup().unreflectGetter(field);
//                Holder.setter = MethodHandles.lookup().unreflectSetter(field);
            }

            Holder.field.set(target, Holder.field.get(this));
//            Holder.setter.invoke(target, Holder.getter.invoke(this));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
