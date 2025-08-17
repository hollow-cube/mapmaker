package net.hollowcube.mapmaker.editor.parkour;

import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.parkour.block.BouncePadBlock;
import net.minestom.server.instance.block.Block;

public class BouncePadEditor {

    public static final ItemHandler ITEM = new BlockItemHandler(BouncePadBlock::new,
            Block.CHERRY_PRESSURE_PLATE, "bounce_pad");

}
