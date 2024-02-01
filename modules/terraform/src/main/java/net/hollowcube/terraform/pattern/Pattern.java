package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public interface Pattern {

    static @NotNull Pattern air() {
        class Holder {
            static final Pattern AIR = (world, blockPosition) -> Block.AIR;
        }
        return Holder.AIR;
    }

    static @NotNull Pattern block(@NotNull Block block) {
        return (world, blockPosition) -> block;
    }

    @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition);

}
