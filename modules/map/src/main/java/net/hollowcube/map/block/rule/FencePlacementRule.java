package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FencePlacementRule extends BlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    public FencePlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var block = updateState.currentBlock();
        var blockPosition = updateState.blockPosition();
        for (var blockFace : HORIZONTAL_FACES) {
            var neighbor = updateState.instance().getBlock(blockPosition.relative(blockFace));

            var canConnect = neighbor.isSolid() && !isPane(neighbor) && checkGateConnection(neighbor, blockFace);
            block = block.withProperty(blockFace.name().toLowerCase(), String.valueOf(canConnect));
        }
        return block;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

    private boolean isPane(@NotNull Block block) {
        return BlockTags.PANES.contains(block.id());
    }

    private boolean checkGateConnection(@NotNull Block block, @NotNull BlockFace fenceFace) {
        if (!BlockTags.MINECRAFT_FENCE_GATES.contains(block.namespace()))
            return true;

        var facing = BlockFace.valueOf(block.getProperty("facing").toUpperCase());
        facing = HORIZONTAL_FACES.get((HORIZONTAL_FACES.indexOf(facing) + 1) % 4); // Get clockwise direction of gate
        return facing.isSimilar(fenceFace);
    }

}
