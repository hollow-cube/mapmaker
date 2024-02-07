package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("UnstableApiUsage")
public class RandomStatePattern implements Pattern {
    private final Collection<Block> possibleStates;

    public RandomStatePattern(int blockId) {
        possibleStates = Objects.requireNonNull(Block.fromBlockId(blockId)).possibleStates();
    }

    @Override
    public @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition) {
        var state = ThreadLocalRandom.current().nextInt(possibleStates.size());
        return possibleStates.stream().skip(state).findFirst().orElse(Block.AIR);
    }
}
