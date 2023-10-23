package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClickFacingPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_FACING = "facing";

    private final boolean allowUp;
    private final boolean invert;

    public ClickFacingPlacementRule(@NotNull Block block, boolean allowUp, boolean invert) {
        super(block);
        this.allowUp = allowUp;
        this.invert = invert;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), invert ? BlockFace.TOP : BlockFace.BOTTOM);
        return block.withProperty(PROP_FACING, directionFromBlockFace(blockFace));
    }

    private @NotNull String directionFromBlockFace(@NotNull BlockFace blockFace) {
        String up = invert ? "down" : "up", down = invert ? "up" : "down";
        return switch (blockFace) {
            case BOTTOM -> allowUp ? down : "down";
            case TOP -> up;
            case NORTH, SOUTH, EAST, WEST -> (invert ? blockFace.getOppositeFace() : blockFace).name().toLowerCase();
        };
    }
}
