package net.hollowcube.terraform.task.edit;

import net.hollowcube.terraform.task.Task;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import net.minestom.server.snapshot.InstanceSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

record WorldViewInstanceSnapshot(
        @NotNull Task task,
        @NotNull InstanceSnapshot snapshot,
        @NotNull WorldBorder border,
        int worldMin, int worldMax
) implements WorldView {

    public WorldViewInstanceSnapshot(@NotNull Task task, @NotNull InstanceSnapshot snapshot, @NotNull WorldBorder border) {
        this(task, snapshot, border, snapshot.dimensionType().getMinY(), snapshot.dimensionType().getMaxY());
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return WorldViewInstance.fastBorderTest(border, x, y, z, worldMin, worldMax);
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return snapshot.getBlock(x, y, z);
    }

}
