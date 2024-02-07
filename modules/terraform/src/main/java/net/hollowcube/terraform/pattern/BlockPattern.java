package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public record BlockPattern(
        @NotNull Block block
) implements Pattern {
    static final BlockPattern AIR = new BlockPattern(Block.AIR);

    @Override
    public @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition) {
        return block;
    }
}
