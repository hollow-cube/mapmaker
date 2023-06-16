package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FacingAllAxisPlacementRule extends BlockPlacementRule {
    private static final String PROP_FACING = "facing";

    public FacingAllAxisPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        String facing;

        var pitch = placementState.playerPosition().pitch();
        if (pitch < -45.0) facing = "down";
        else if (pitch > 45.0) facing = "up";
        else facing = BlockFace.fromYaw(placementState.playerPosition().yaw())
                    .getOppositeFace().name().toLowerCase();

        return block.withProperty(PROP_FACING, facing);
    }
}
