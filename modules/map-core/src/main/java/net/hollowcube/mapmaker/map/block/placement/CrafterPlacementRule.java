package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.terraform.util.math.DirectionUtil;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class CrafterPlacementRule extends BaseBlockPlacementRule {
    public CrafterPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        var orientation = "east_up";
        if (placement.playerPosition() != null && placement.blockFace() != null) {
            var direction = DirectionUtil.fromView(placement.playerPosition()).opposite();
            var horizontal = DirectionUtil.fromYaw(placement.playerPosition());

            orientation = switch (direction) {
                case UP -> "up_" + horizontal.name().toLowerCase(Locale.ROOT);
                case DOWN -> "down_" + horizontal.opposite().name().toLowerCase(Locale.ROOT);
                default -> direction.name().toLowerCase(Locale.ROOT) + "_up";
            };
        }
        return this.block.withProperty("orientation", orientation);
    }
}
