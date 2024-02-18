package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class JigsawPlacementRule extends BaseBlockPlacementRule {
    public JigsawPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        var orientation = "up_east";
        if (placement.playerPosition() != null && placement.blockFace() != null) {
            var horizontal = BlockFace.fromYaw(placement.playerPosition().yaw())
                    .getOppositeFace().name().toLowerCase(Locale.ROOT);

            if (placement.blockFace() == BlockFace.TOP || placement.blockFace() == BlockFace.BOTTOM) {
                var vertical = placement.blockFace() == BlockFace.BOTTOM ? "down" : "up";
                orientation = vertical + "_" + horizontal;
            } else {
                orientation = horizontal + "_up";
            }

        }
        return this.block.withProperty("orientation", orientation);
    }
}
