package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ScaffoldingPlacementRule extends BaseBlockPlacementRule {
    public ScaffoldingPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        return getState(placement.instance(), placement.placePosition(), this.block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull BlockPlacementRule.UpdateState update) {
        if (update.fromFace() != BlockFace.BOTTOM) return update.currentBlock();
        return getState(update.instance(), update.blockPosition(), update.currentBlock());
    }

    private @NotNull Block getState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block current) {
        var blockBelow = instance.getBlock(blockPosition.add(0, -1, 0), Block.Getter.Condition.TYPE);
        var isBottom = !blockBelow.registry().collisionShape().isFaceFull(BlockFace.TOP);
        return current.withProperty("bottom", String.valueOf(isBottom));
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
