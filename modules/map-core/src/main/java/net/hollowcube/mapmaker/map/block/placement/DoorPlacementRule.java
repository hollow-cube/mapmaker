package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class DoorPlacementRule extends BaseBlockPlacementRule {
    public DoorPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var instance = placementState.instance();
        var blockPosition = placementState.placePosition();

        var abovePosition = blockPosition.add(0, 1, 0);
        if (!instance.getBlock(abovePosition, Block.Getter.Condition.TYPE).isAir())
            return null;

        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var facing = BlockFace.fromYaw(playerPosition.yaw());
        var isLeftHinge = computeHinge(facing, Objects.requireNonNullElse(placementState.cursorPosition(), Vec.ZERO));
        //todo doors need to connect to other doors
        var placedBlock = block.withProperties(Map.of(
                "facing", facing.name().toLowerCase(),
                "hinge", isLeftHinge ? "left" : "right")
        );

        var aboveBlock = placedBlock.withProperty("half", "upper");
        placeOtherBlock(instance, abovePosition, aboveBlock);

        return placedBlock;
    }

    private boolean computeHinge(@NotNull BlockFace blockFace, @NotNull Point cursorPosition) {
        return switch (blockFace) {
            case NORTH -> cursorPosition.x() < 0.5;
            case SOUTH -> cursorPosition.x() > 0.5;
            case WEST -> cursorPosition.z() > 0.5;
            case EAST -> cursorPosition.z() < 0.5;
            default -> false;
        };
    }
}
