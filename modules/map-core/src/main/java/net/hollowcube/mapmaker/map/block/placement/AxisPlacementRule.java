package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AxisPlacementRule extends WaterloggedPlacementRule {
    private static final String PROP_AXIS = "axis";

    private final boolean canBeWaterlogged;

    public AxisPlacementRule(@NotNull Block block) {
        super(block);
        this.canBeWaterlogged = block.properties().containsKey("waterlogged");
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        final Block result = block.withProperty(PROP_AXIS, switch (blockFace) {
            case WEST, EAST -> "x";
            case SOUTH, NORTH -> "z";
            case TOP, BOTTOM -> "y";
        });
        return canBeWaterlogged ? result.withProperty("waterlogged", waterlogged(placementState)) : result;
    }
}
