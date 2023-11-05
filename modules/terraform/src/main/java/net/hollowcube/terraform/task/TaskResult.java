package net.hollowcube.terraform.task;

import net.hollowcube.terraform.buffer.BlockBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record TaskResult(
        @NotNull BlockBuffer undoBuffer,
        @NotNull BlockBuffer redoBuffer,
        long blocksChanged,
        @NotNull Set<@NotNull String> attributes
) {

    public TaskResult {
        attributes = Set.copyOf(attributes);
    }

    public boolean hasAttribute(@NotNull String attribute) {
        return attributes.contains(attribute);
    }
    
}
