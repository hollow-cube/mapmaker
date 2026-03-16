package net.hollowcube.mapmaker.map.block.ghost;

import net.minestom.server.instance.block.Block;

import java.util.Map;

public interface BlockUpdateTask {
    Map.Entry<Integer, Block> STOP = Map.entry(0, Block.AIR);

    // Number of ticks to wait
    Map.Entry<Integer, Block> execute(Block block);

}
