package net.hollowcube.common.util;

import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public final class BlockUtil {

    public static @NotNull Block fromString(@NotNull String blockState) {
        return ArgumentBlockState.staticParse(blockState);
    }

    public static @NotNull String toString(@NotNull Block block) {
        var builder = new StringBuilder(block.name());
        if (block.properties().isEmpty()) return builder.toString();

        builder.append('[');
        for (var entry : block.properties().entrySet()) {
            builder.append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append(',');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(']');

        return builder.toString();
    }

}
