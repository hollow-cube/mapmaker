package net.hollowcube.terraform.task;

import net.hollowcube.terraform.action.edit.WorldView;
import net.hollowcube.terraform.buffer.BlockBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * A ComputeFunc is the lowest level primitive responsible for the compute phase of a {@link Task}.
 *
 * <p>TODO add some higher level abstractions around this</p>
 */
@FunctionalInterface
public interface ComputeFunc {

    @NotNull BlockBuffer exec(@NotNull WorldView world);

}
