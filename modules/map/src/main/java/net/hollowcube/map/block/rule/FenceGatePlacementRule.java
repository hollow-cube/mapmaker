package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FenceGatePlacementRule extends FacingPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private static final String PROP_IN_WALL = "in_wall";

    public FenceGatePlacementRule(@NotNull Block block) {
        super(block, false);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var facing = BlockFace.valueOf(block.getProperty("facing").toUpperCase());
        facing = HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facing) + 1) % 4); // Get clockwise direction of gate

        var instance = updateState.instance();
        var pos = updateState.blockPosition();
        var inWall = isWall(instance, pos, facing) || isWall(instance, pos, facing.getOppositeFace());
        return block.withProperty(PROP_IN_WALL, String.valueOf(inWall));
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return super.blockPlace(placementState); // Facing rule
    }

    private boolean isWall(@NotNull Block.Getter instance, @NotNull Point pos, @NotNull BlockFace blockFace) {
        var block = instance.getBlock(pos.relative(blockFace), Block.Getter.Condition.TYPE);
        return BlockTags.MINECRAFT_WALLS.contains(block.namespace());
    }

}
