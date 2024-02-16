package net.hollowcube.map2.block.placement;

import net.hollowcube.map2.block.BlockTags;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class SmallFlowerPlacementRule extends BaseBlockPlacementRule {
    public SmallFlowerPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition(), Block.Getter.Condition.TYPE);
        if (existingBlock.id() == Block.FLOWER_POT.id()) {
            if (BlockTags.POTTABLE_FLOWERS.contains(block.namespace()))
                return Block.fromNamespaceId("minecraft:potted_" + block.namespace().path());
            return null;
        } else {
            return block;
        }
    }
}
