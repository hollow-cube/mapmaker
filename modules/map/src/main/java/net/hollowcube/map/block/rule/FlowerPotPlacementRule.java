package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlowerPotPlacementRule extends BaseBlockPlacementRule {
    protected FlowerPotPlacementRule() {
        super(Block.FLOWER_POT);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return placementState.block();
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        var block = replacement.block();
        return BlockTags.MINECRAFT_SMALL_FLOWERS.contains(block.namespace());
    }
}
