package net.hollowcube.terraform.legacy.buffer;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.task.Task;
import net.hollowcube.terraform.legacy.task.edit.WorldView;
import net.hollowcube.terraform.legacy.util.ThreadUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds an {@link EditBlockBuffer} directly, so the task's buffer needs no conversion before apply.
 */
final class EditBlockBufferBuilder implements BlockBuffer.Builder {
    private final @Nullable WorldView world;
    private final EditBuffer.Builder edit = EditBuffer.builder();

    // Inclusive bounds every set must fall in, unchecked when unbounded
    private final boolean bounded;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private boolean hasBorderTaint = false;

    EditBlockBufferBuilder(@Nullable WorldView world) {
        this.world = world;
        this.bounded = false;
        this.minX = this.minY = this.minZ = this.maxX = this.maxY = this.maxZ = 0;
    }

    EditBlockBufferBuilder(@Nullable WorldView world, @NotNull Point pos1, @NotNull Point pos2) {
        this.world = world;
        this.bounded = true;
        this.minX = Math.min(pos1.blockX(), pos2.blockX());
        this.minY = Math.min(pos1.blockY(), pos2.blockY());
        this.minZ = Math.min(pos1.blockZ(), pos2.blockZ());
        this.maxX = Math.max(pos1.blockX(), pos2.blockX());
        this.maxY = Math.max(pos1.blockY(), pos2.blockY());
        this.maxZ = Math.max(pos1.blockZ(), pos2.blockZ());
    }

    @Override
    public void set(int x, int y, int z, int value) throws InterruptedException {
        if (accepts(x, y, z)) edit.setBlockState(x, y, z, value);
    }

    @Override
    public void set(int x, int y, int z, @NotNull Block value) throws InterruptedException {
        if (accepts(x, y, z)) edit.setBlockState(x, y, z, value);
    }

    @Override
    public @NotNull BlockBuffer build() throws InterruptedException {
        ThreadUtil.testInterrupt();
        return new EditBlockBuffer(edit.build());
    }

    /**
     * Whether the cell may be set: throws outside the builder's bounds, and drops (tainting the task) cells
     * outside the world border.
     */
    private boolean accepts(int x, int y, int z) throws InterruptedException {
        ThreadUtil.testInterrupt();
        Check.argCondition(bounded && (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ),
                "Point ({0}, {1}, {2}) is not within the bounds of this builder", x, y, z);

        if (world == null || world.contains(x, y, z)) return true;
        if (!hasBorderTaint) {
            hasBorderTaint = true;
            world.task().addAttribute(Task.ATT_BORDER_TAINT);
        }
        return false;
    }
}
