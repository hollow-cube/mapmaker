package net.hollowcube.terraform.task.edit;

import net.hollowcube.terraform.task.Task;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record WorldViewInstance(
        @NotNull Task task,
        @NotNull Instance instance,
        int worldMin, int worldMax
) implements WorldView {

    public WorldViewInstance(@NotNull Task task, @NotNull Instance instance) {
        this(task, instance, instance.getDimensionType().getMinY(), instance.getDimensionType().getMaxY());
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return instance.getBlock(x, y, z);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return fastBorderTest(instance.getWorldBorder(), worldMin, worldMax, x, y, z);
    }

    static boolean fastBorderTest(@NotNull WorldBorder border, int worldMin, int worldMax, int x, int y, int z) {
        final double radius = border.getDiameter() / 2d;
        // Use < on the positive end because we have block positions and it would include one outside the actual border.
        final boolean checkX = x < border.getCenterX() + radius && x >= border.getCenterX() - radius;
        final boolean checkZ = z < border.getCenterZ() + radius && z >= border.getCenterZ() - radius;
        final boolean checkY = y >= worldMin && y < worldMax;
        return checkX && checkZ && checkY;
    }
}
