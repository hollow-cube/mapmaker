package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FacingAllAxisPlacementRule extends BaseBlockPlacementRule {
    private static final String PROP_FACING = "facing";

    private final boolean invert;

    public FacingAllAxisPlacementRule(@NotNull Block block, boolean invert) {
        super(block);
        this.invert = invert;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);

        String facing;
        var pitch = playerPosition.pitch();
        if (pitch < -45.0) facing = invert ? "up" : "down";
        else if (pitch > 45.0) facing = invert ? "down" : "up";
        else {
            var facingFace = BlockFace.fromYaw(playerPosition.yaw());
            if (!invert) facingFace = facingFace.getOppositeFace();
            facing = facingFace.name().toLowerCase();
        }

        return block.withProperty(PROP_FACING, facing);
    }

}
