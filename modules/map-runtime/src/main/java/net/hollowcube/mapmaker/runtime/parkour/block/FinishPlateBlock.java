package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.runtime.parkour.node.LegacyTriggers;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

public class FinishPlateBlock implements BlockHandler, PressurePlateBlock {
    private static final Key KEY = Key.key("mapmaker:finish_plate");

    public static final FinishPlateBlock INSTANCE = new FinishPlateBlock();

    private FinishPlateBlock() {}

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Trigger createTrigger(BlockVec position, Block block) {
        var id = String.format(
            "mapmaker:finish_plate/%d_%d_%d",
            position.blockX(),
            position.blockY(),
            position.blockZ()
        );
        return LegacyTriggers.finish(id, position);
    }

}
