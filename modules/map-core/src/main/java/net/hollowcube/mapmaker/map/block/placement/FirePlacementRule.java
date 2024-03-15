package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

public class FirePlacementRule extends BaseBlockPlacementRule {
    public FirePlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        var instance = placement.instance();
        var blockPosition = placement.placePosition();
        var placeFace = Objects.requireNonNullElse(placement.blockFace(), BlockFace.TOP);

        // If there is a block below, always place the default state
        var belowBlock = instance.getBlock(blockPosition.add(0, -1, 0));
        if (placement.blockFace() == BlockFace.TOP || belowBlock.isSolid()) return block;

        // Otherwise, place with the given face set
        var faceProperty = switch (placeFace) {
            case NORTH, SOUTH, EAST, WEST -> placeFace.getOppositeFace().name().toLowerCase(Locale.ROOT);
            default -> "up";
        };
        return block.withProperty(faceProperty, "true");
    }

    @Override
    public boolean isSelfReplaceable(@NotNull BlockPlacementRule.Replacement replacement) {
        return true;
    }
}
