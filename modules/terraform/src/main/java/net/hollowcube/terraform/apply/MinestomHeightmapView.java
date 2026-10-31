package net.hollowcube.terraform.apply;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.heightmap.Heightmap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

/// A Minestom [Heightmap] as a [HeightmapView].
///
/// Contains terrible hackery :-( (TODO: upstream some improvements). Needs
/// `--add-opens net.minestom.server/net.minestom.server.instance.heightmap` under JPMS.
final class MinestomHeightmapView implements HeightmapView {
    private static final VarHandle HEIGHTS;
    private static final MethodHandle CHECK_BLOCK;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Heightmap.class, MethodHandles.lookup());
            HEIGHTS = lookup.findVarHandle(Heightmap.class, "heights", short[].class);
            CHECK_BLOCK = lookup.findVirtual(
                Heightmap.class, "checkBlock",
                MethodType.methodType(boolean.class, Block.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Heightmap heightmap;
    private final short[] heights;
    private final int minHeight;

    static List<HeightmapView> of(Chunk chunk) {
        return List.of(
            new MinestomHeightmapView(chunk, chunk.motionBlockingHeightmap()),
            new MinestomHeightmapView(chunk, chunk.worldSurfaceHeightmap())
        );
    }

    private MinestomHeightmapView(Chunk chunk, Heightmap heightmap) {
        // Computes a lazy heightmap before the engine writes into its array.
        heightmap.getHeight(0, 0);
        this.heightmap = heightmap;
        this.heights = (short[]) HEIGHTS.get(heightmap);
        this.minHeight = chunk.getInstance().getCachedDimensionType().minY() - 1;
    }

    @Override
    public boolean bottomUp() {
        return false;
    }

    @Override
    public boolean matches(int stateId) {
        try {
            return (boolean) CHECK_BLOCK.invokeExact(heightmap, Block.fromStateId(stateId));
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int height(int x, int z) {
        return heights[z << 4 | x] + minHeight;
    }

    @Override
    public void setHeight(int x, int z, int y) {
        heights[z << 4 | x] = (short) (y - minHeight);
    }
}
