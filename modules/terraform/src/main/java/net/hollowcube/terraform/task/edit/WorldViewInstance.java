package net.hollowcube.terraform.task.edit;

import net.hollowcube.terraform.task.Task;
import net.minestom.server.MinecraftServer;
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
        this(task, instance, MinecraftServer.getDimensionTypeRegistry().get(instance.getDimensionType()).minY(), MinecraftServer.getDimensionTypeRegistry().get(instance.getDimensionType()).maxY());
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
        final double radius = border.diameter() / 2d;
        // Use < on the positive end because we have block positions and it would include one outside the actual border.
        final boolean checkX = x < border.centerX() + radius && x >= border.centerX() - radius;
        final boolean checkZ = z < border.centerZ() + radius && z >= border.centerZ() - radius;
        final boolean checkY = y >= worldMin && y < worldMax;
        return checkX && checkZ && checkY;
    }
}
