package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TallFlowerPlacementRule extends BlockPlacementRule {

    public TallFlowerPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        //todo bad code using instance directly
        if (!(placementState.instance() instanceof Instance instance)) return null;

        var posAbove = placementState.placePosition().add(0, 1, 0);
        if (!instance.getBlock(posAbove, Block.Getter.Condition.TYPE).isAir())
            return null;

        instance.setBlock(posAbove, this.block.withProperty("half", "upper"));
        return this.block;
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        return true;
    }
}
