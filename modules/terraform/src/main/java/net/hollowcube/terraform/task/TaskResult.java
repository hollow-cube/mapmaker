package net.hollowcube.terraform.task;

import net.hollowcube.terraform.buffer.BlockBuffer;
import org.jetbrains.annotations.NotNull;

public record TaskResult(
        @NotNull BlockBuffer undoBuffer,
        @NotNull BlockBuffer redoBuffer,
        long blocksChanged
) {
}
