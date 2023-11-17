package net.hollowcube.terraform.task;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.task.edit.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A ComputeFunc is the lowest level primitive responsible for the compute phase of a {@link Task}.
 *
 * <p>TODO add some higher level abstractions around this</p>
 */
@FunctionalInterface
public interface ComputeFunc {

    static @NotNull ComputeFunc set(@NotNull Region region, @NotNull Pattern pattern) {
        return (task, world) -> {
            var buffer = BlockBuffer.builder(world, region.min(), region.max());
            for (var pos : region) {
                //todo block entities
                buffer.set(pos, pattern.blockAt(world, pos));
            }
            return buffer.build();
        };
    }

    static @NotNull ComputeFunc replace(@NotNull Region region, @Nullable Mask mask, @NotNull Pattern pattern) {
        return (task, world) -> {
            var buffer = BlockBuffer.builder(world, region.min(), region.max());
            for (var pos : region) {
                var block = world.getBlock(pos);
                if (!mask.test(world, pos, block)) continue;
                buffer.set(pos, pattern.blockAt(world, pos).stateId());
                //todo block entities
            }
            return buffer.build();
        };
    }

    @NotNull BlockBuffer exec(@NotNull Task task, @NotNull WorldView world);

}
