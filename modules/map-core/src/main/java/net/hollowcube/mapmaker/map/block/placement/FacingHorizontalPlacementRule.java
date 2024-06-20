package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

public class FacingHorizontalPlacementRule extends WaterloggedPlacementRule {
    private final boolean invert;
    private final boolean canBeWaterlogged;

    public FacingHorizontalPlacementRule(@NotNull Block block, boolean invert) {
        super(block);
        this.invert = invert;
        this.canBeWaterlogged = block.properties().containsKey("waterlogged");
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = BlockFace.fromYaw(playerPosition.yaw());
        if (invert) facing = facing.getOppositeFace();
        final Block result = block.withProperty("facing", facing.name().toLowerCase());
        return canBeWaterlogged ? result.withProperty("waterlogged", waterlogged(placementState)) : result;
    }
}
