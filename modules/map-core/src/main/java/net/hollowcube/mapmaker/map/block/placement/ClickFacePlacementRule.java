package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClickFacePlacementRule extends BaseBlockPlacementRule {
    public static final String PROP_FACING = "facing";

    private final boolean useAltVertical;

    public ClickFacePlacementRule(@NotNull Block block) {
        this(block, false);
    }

    public ClickFacePlacementRule(@NotNull Block block, boolean useAltVertical) {
        super(block);
        this.useAltVertical = useAltVertical;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        return block.withProperty(PROP_FACING, switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> blockFace.name().toLowerCase();
            case TOP -> useAltVertical ? "up" : "top";
            case BOTTOM -> useAltVertical ? "down" : "bottom";
        });
    }
}
