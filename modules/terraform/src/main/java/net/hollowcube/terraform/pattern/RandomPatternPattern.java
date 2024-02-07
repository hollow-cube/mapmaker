package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RandomPatternPattern(
        @NotNull List<Entry> entries,
        int total
) implements Pattern {

    public record Entry(int weight, @NotNull Pattern pattern) {
    }

    @Override
    public @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition) {
        var entry = world.random().nextInt(total);
        for (var e : entries) {
            entry -= e.weight;
            if (entry < 0) {
                return e.pattern.blockAt(world, blockPosition);
            }
        }
        return Block.AIR; // Unreachable
    }

}
