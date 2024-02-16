package net.hollowcube.map2.block.placement;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class TallFlowerPlacementRule extends BaseBlockPlacementRule {

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
