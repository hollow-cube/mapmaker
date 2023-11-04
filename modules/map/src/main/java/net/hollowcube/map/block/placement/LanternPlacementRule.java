package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class LanternPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_HANGING = "hanging";

    public LanternPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var instance = placementState.instance();

        // Try to place on the block below
        var blockBelow = instance.getBlock(placementState.placePosition().add(0, -1, 0), Block.Getter.Condition.TYPE);
        if (blockBelow.isSolid()) return block.withProperty(PROP_HANGING, "false");

        // Try to place on the block above
        var blockAbove = instance.getBlock(placementState.placePosition().add(0, 1, 0), Block.Getter.Condition.TYPE);
        if (blockAbove.isSolid()) return block.withProperty(PROP_HANGING, "true");

        return block;
    }
}
