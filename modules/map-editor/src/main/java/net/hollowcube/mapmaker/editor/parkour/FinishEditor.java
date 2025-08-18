package net.hollowcube.mapmaker.editor.parkour;

import net.hollowcube.mapmaker.map.entity.object.ObjectEntityEditor;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.runtime.parkour.block.FinishPlateBlock;
import net.minestom.server.instance.block.Block;

public class FinishEditor {

    public static final BlockItemHandler PLATE_ITEM = new BlockItemHandler(() -> FinishPlateBlock.INSTANCE,
            Block.LIGHT_WEIGHTED_PRESSURE_PLATE, "finish_plate");

    public static final ObjectEntityEditor MARKER_EDITOR = (_, _) -> true;

}
