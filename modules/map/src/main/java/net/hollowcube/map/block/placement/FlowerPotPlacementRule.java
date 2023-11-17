package net.hollowcube.map.block.placement;

import net.hollowcube.map.block.BlockTags;
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
        if (BlockTags.SMALL_FLOWERS.contains(block.namespace())) {
            return Block.fromNamespaceId("minecraft:potted_" + block.namespace().path());
        }

        return placementState.block();
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        var block = replacement.material().block();
        return BlockTags.SMALL_FLOWERS.contains(block.namespace());
    }
}
