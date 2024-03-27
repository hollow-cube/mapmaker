package net.hollowcube.mapmaker.map.instance;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.util.function.Predicate;

public class Heightmaps {

    public static final int WORLD_SURFACE = 0;
    public static final int MOTION_BLOCKING = 1;
    public static final int WORLD_BOTTOM = 2;

    private final Heightmap[] heightmaps = new Heightmap[3];

    public Heightmaps(@NotNull Chunk chunk) {
        heightmaps[WORLD_SURFACE] = new Heightmap(chunk, Heightmap.SURFACE, Predicate.not(Block::isAir));
        heightmaps[MOTION_BLOCKING] = new Heightmap(chunk, Heightmap.SURFACE, this::isMotionBlocking);
        heightmaps[WORLD_BOTTOM] = new Heightmap(chunk, Heightmap.BOTTOM, Predicate.not(Block::isAir));
    }

    public int get(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap, int x, int z) {
        return heightmaps[heightmap].get(x, z);
    }

    public void update(int x, int y, int z, @NotNull Block block) {
        for (var heightmap : heightmaps) {
            heightmap.update(x & 0xF, y, z & 0xF, block);
        }
    }

    public void load(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap, int[] data) {
        heightmaps[heightmap].load(data);
    }

    public int[] save(@MagicConstant(valuesFromClass = Heightmaps.class) int heightmap) {
        return heightmaps[heightmap].save();
    }

    public @NotNull NBTCompound getProtocolData() {
        var compound = new MutableNBTCompound();
        var worldSurface = heightmaps[WORLD_SURFACE].encode();
        if (worldSurface != null) compound.set("WORLD_SURFACE", NBT.LongArray(worldSurface));
        var motionBlocking = heightmaps[MOTION_BLOCKING].encode();
        if (motionBlocking != null) compound.set("MOTION_BLOCKING", NBT.LongArray(motionBlocking));
        return compound.toCompound();
    }

    private boolean isMotionBlocking(@NotNull Block block) {
        // This is from the client. nice one mojang!
        return block.id() != Block.COBWEB.id() && block.id() != Block.BAMBOO_SAPLING.id() && block.isSolid();
    }
}
