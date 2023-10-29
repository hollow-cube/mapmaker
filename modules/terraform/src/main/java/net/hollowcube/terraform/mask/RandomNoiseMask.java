package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Matches a given percentage of the calls.
 *
 * @param chance the chance of matching, from 0 to 1
 */
public record RandomNoiseMask(double chance) implements Mask {
    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
