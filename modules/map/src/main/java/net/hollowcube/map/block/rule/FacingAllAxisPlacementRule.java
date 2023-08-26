package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FacingAllAxisPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_FACING = "facing";

    public FacingAllAxisPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);

        String facing;
        var pitch = playerPosition.pitch();
        if (pitch < -45.0) facing = "down";
        else if (pitch > 45.0) facing = "up";
        else facing = BlockFace.fromYaw(playerPosition.yaw()).getOppositeFace().name().toLowerCase();

        return block.withProperty(PROP_FACING, facing);
    }

}
