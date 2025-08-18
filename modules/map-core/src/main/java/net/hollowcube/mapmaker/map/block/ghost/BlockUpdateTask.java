package net.hollowcube.mapmaker.map.block.ghost;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface BlockUpdateTask {
    Map.Entry<Integer, Block> STOP = Map.entry(0, Block.AIR);

    // Number of ticks to wait
    @NotNull Map.Entry<Integer, Block> execute(@NotNull Block block);

}
