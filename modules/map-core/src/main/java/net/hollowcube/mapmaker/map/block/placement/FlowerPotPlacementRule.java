package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlowerPotPlacementRule extends BaseBlockPlacementRule {
    public FlowerPotPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var block = placementState.block();
        if (BlockTags.POTTABLE_FLOWERS.contains(block.key())) {
            return Block.fromKey("minecraft:potted_" + block.key().value());
        }

        return placementState.block();
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        var block = replacement.material().block();
        return BlockTags.POTTABLE_FLOWERS.contains(block.key());
    }
}
