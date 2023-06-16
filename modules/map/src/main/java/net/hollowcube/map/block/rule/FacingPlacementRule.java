package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FacingPlacementRule extends BlockPlacementRule {
    private final boolean invert;

    public FacingPlacementRule(@NotNull Block block, boolean invert) {
        super(block);
        this.invert = invert;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var facing = BlockFace.fromYaw(placementState.playerPosition().yaw());
        if (invert) facing = facing.getOppositeFace();
        return block.withProperty("facing", facing.name().toLowerCase());
    }
}
