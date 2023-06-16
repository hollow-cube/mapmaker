package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlowerPotPlacementRule extends BlockPlacementRule {
    protected FlowerPotPlacementRule() {
        super(Block.FLOWER_POT);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return placementState.block();
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Block block, @NotNull BlockFace blockFace, @NotNull Point cursorPosition) {
        return BlockTags.MINECRAFT_SMALL_FLOWERS.contains(block.namespace());
    }
}
