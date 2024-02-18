package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

public class FacingHorizontalPlacementRule extends BaseBlockPlacementRule {
    private final boolean invert;

    public FacingHorizontalPlacementRule(@NotNull Block block, boolean invert) {
        super(block);
        this.invert = invert;
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = BlockFace.fromYaw(playerPosition.yaw());
        if (invert) facing = facing.getOppositeFace();
        return block.withProperty("facing", facing.name().toLowerCase());
    }
}
