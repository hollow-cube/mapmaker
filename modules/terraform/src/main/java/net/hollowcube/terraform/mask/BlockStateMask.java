package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record BlockStateMask(@NotNull Map<String, String> properties, boolean strict) implements Mask {
    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        for (var entry : properties.entrySet()) {
            var property = block.getProperty(entry.getKey());
            if (property == null) {
                if (strict) return false;
                else continue;
            }

            if (!property.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
