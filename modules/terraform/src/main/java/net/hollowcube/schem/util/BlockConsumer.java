package net.hollowcube.schem.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface BlockConsumer {

    void accept(@NotNull Point blockPosition, @NotNull Block block);

}
