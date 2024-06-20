package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class SmallFlowerPlacementRule extends WaterloggedPlacementRule {
    private final boolean canBeWaterlogged;

    public SmallFlowerPlacementRule(@NotNull Block block) {
        super(block);
        this.canBeWaterlogged = block.properties().containsKey("waterlogged");
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition(), Block.Getter.Condition.TYPE);
        if (existingBlock.id() == Block.FLOWER_POT.id()) {
            if (BlockTags.POTTABLE_FLOWERS.contains(block.namespace()))
                return Block.fromNamespaceId("minecraft:potted_" + block.namespace().path());
            return null;
        }

        return canBeWaterlogged ? block.withProperty("waterlogged", waterlogged(placementState)) : block;
    }
}
