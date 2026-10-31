package net.hollowcube.terraform.util;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

import java.util.Objects;

public final class BlockUtil {
    private static final int XZ_MAX = 1 << 25; // 26 signed bits
    private static final int Y_MAX = 1 << 11; // 12 signed bits

    /// Whether the state id alone fully describes the block: no NBT, no handler, and not a block-entity
    /// type. Anything else has to be stored as the full [Block], not just its state id.
    public static boolean isPlainState(Block block) {
        return !block.hasNbt() && block.handler() == null && block.blockEntityType() == null;
    }

    /// Checks block equality looking at just the handler ids not the object instances
    public static boolean sameBlock(Block a, Block b) {
        if (a.stateId() != b.stateId() || !Objects.equals(a.nbt(), b.nbt())) return false;
        BlockHandler ha = a.handler(), hb = b.handler();
        if (ha == null || hb == null) return ha == hb;
        return ha.getKey().equals(hb.getKey());
    }

    /// Absolute xyz packed into a long. Stores up to −33,554,432 to 33,554,431 on x/z, −2,048 to 2,047 on y
    /// which is within the vanilla/sane limit of +/-30m and 4096 height.
    ///
    /// Notably not the same as Minestom [net.minestom.server.coordinate.CoordConversion#sectionIndex],
    /// which is 21 bits per axis (which underflows x/z but Minestom uses it for chunk-local coords).
    /// TODO: should look into changing Minestom representation so its more useful.
    ///
    /// @see #blockPosGetX(long)
    /// @see #blockPosGetY(long)
    /// @see #blockPosGetZ(long)
    public static long packBlockPos(int x, int y, int z) {
        if (x < -XZ_MAX || x >= XZ_MAX || z < -XZ_MAX || z >= XZ_MAX || y < -Y_MAX || y >= Y_MAX) {
            throw new IllegalArgumentException("position out of range: " + x + ", " + y + ", " + z);
        }
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /// The x of a [#packBlockPos(int, int, int)] key.
    public static int blockPosGetX(long key) {
        return (int) (key >> 38);
    }

    /// The y of a [#packBlockPos(int, int, int)] key.
    public static int blockPosGetY(long key) {
        return (int) (key << 52 >> 52);
    }

    /// The z of a [#packBlockPos(int, int, int)] key.
    public static int blockPosGetZ(long key) {
        return (int) (key << 26 >> 38);
    }

    private BlockUtil() {}
}
