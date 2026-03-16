package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClickFacingPlacementRule extends WaterloggedPlacementRule {
    private static final String PROP_FACING = "facing";

    private final boolean allowUp;
    private final boolean invert;
    private final boolean canBeWaterlogged;

    public ClickFacingPlacementRule(Block block, boolean allowUp, boolean invert) {
        super(block);
        this.allowUp = allowUp;
        this.invert = invert;
        this.canBeWaterlogged = block.properties().containsKey("waterlogged");
    }

    @Override
    public @Nullable Block blockPlace(PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), invert ? BlockFace.TOP : BlockFace.BOTTOM);
        final Block result = block.withProperty(PROP_FACING, directionFromBlockFace(blockFace));
        return canBeWaterlogged ? result.withProperty("waterlogged", waterlogged(placementState)) : result;
    }

    private String directionFromBlockFace(BlockFace blockFace) {
        String up = invert ? "down" : "up", down = invert ? "up" : "down";
        return switch (blockFace) {
            case BOTTOM -> allowUp ? down : "down";
            case TOP -> up;
            case NORTH, SOUTH, EAST, WEST -> (invert ? blockFace.getOppositeFace() : blockFace).name().toLowerCase();
        };
    }
}
