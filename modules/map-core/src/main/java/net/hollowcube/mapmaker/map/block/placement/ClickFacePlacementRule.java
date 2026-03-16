package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClickFacePlacementRule extends WaterloggedPlacementRule {
    public static final String PROP_FACING = "facing";

    private final boolean useAltVertical;
    private final boolean canBeWaterlogged;

    public ClickFacePlacementRule(Block block) {
        this(block, false);
    }

    public ClickFacePlacementRule(Block block, boolean useAltVertical) {
        super(block);
        this.useAltVertical = useAltVertical;
        this.canBeWaterlogged = block.properties().containsKey("waterlogged");
    }

    @Override
    public @Nullable Block blockPlace(PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        final Block result = block.withProperty(PROP_FACING, switch (blockFace) {
            case NORTH, SOUTH, EAST, WEST -> blockFace.name().toLowerCase();
            case TOP -> useAltVertical ? "up" : "top";
            case BOTTOM -> useAltVertical ? "down" : "bottom";
        });
        return canBeWaterlogged ? result.withProperty("waterlogged", waterlogged(placementState)) : result;
    }
}
