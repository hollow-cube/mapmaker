package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClickFacePlacementRule extends BlockPlacementRule {
    public static final String PROP_FACING = "facing";

    private boolean useAltVertical;

    public ClickFacePlacementRule(@NotNull Block block) {
        this(block, false);
    }

    public ClickFacePlacementRule(@NotNull Block block, boolean useAltVertical) {
        super(block);
        this.useAltVertical = useAltVertical;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = placementState.blockFace();
        return block.withProperty(PROP_FACING, switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> blockFace.name().toLowerCase();
            case TOP -> useAltVertical ? "up" : "top";
            case BOTTOM -> useAltVertical ? "down" : "bottom";
        });
    }
}
