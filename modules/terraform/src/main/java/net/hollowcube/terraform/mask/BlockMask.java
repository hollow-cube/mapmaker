package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record BlockMask(
        int blockId,
        @NotNull Map<String, String> properties
) implements Mask {

    public BlockMask(int blockId) {
        this(blockId, Map.of());
    }

    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        if (blockId != block.id())
            return false;
        for (var entry : properties.entrySet()) {
            var propValue = block.getProperty(entry.getKey());
            if (propValue == null || !propValue.equals(entry.getValue()))
                return false;
        }
        return true;
    }
}
