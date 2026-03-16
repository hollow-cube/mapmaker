package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;

public abstract class BaseBlockPlacementRule extends BlockPlacementRule {
    protected static final BlockFace[] HORIZONTAL = new BlockFace[]{
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    protected BaseBlockPlacementRule(Block block) {
        super(block);
    }

    protected boolean canConnect(Block block) {
        return !(BlockTags.LEAVES.contains(block.key()) ||
            block.id() == Block.BARRIER.id() ||
            block.id() == Block.CARVED_PUMPKIN.id() ||
            block.id() == Block.JACK_O_LANTERN.id() ||
            block.id() == Block.MELON.id() ||
            block.id() == Block.PUMPKIN.id() ||
            BlockTags.SHULKER_BOXES.contains(block.key()));
    }

    @Override
    public int maxUpdateDistance() {
        return 0;
    }

    protected void placeOtherBlock(Block.Getter getter, Point point, Block block) {
        if (getter instanceof Instance instance) {
            instance.setBlock(point, block);
        }
    }

}
