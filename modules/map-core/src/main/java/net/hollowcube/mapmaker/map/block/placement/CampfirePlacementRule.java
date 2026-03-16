package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.UnknownNullability;

public class CampfirePlacementRule extends FacingHorizontalPlacementRule {

    public CampfirePlacementRule(Block block) {
        super(block, false);
    }

    @Override
    public @UnknownNullability Block blockPlace(PlacementState placement) {
        return setSignalFire(placement.instance(), placement.placePosition(), super.blockPlace(placement));
    }

    @Override
    public Block blockUpdate(BlockPlacementRule.UpdateState update) {
        return setSignalFire(update.instance(), update.blockPosition(), update.currentBlock());
    }

    private Block setSignalFire(Block.Getter instance, Point blockPosition, Block block) {
        var belowBlock = instance.getBlock(blockPosition.add(0, -1, 0));
        var isSignalFire = Boolean.toString(belowBlock.id() == Block.HAY_BLOCK.id());
        return block.withProperty("signal_fire", isSignalFire);
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
