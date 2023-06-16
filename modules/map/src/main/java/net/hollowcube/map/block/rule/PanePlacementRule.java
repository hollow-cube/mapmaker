package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PanePlacementRule extends BlockPlacementRule {
    private static final BlockFace[] HORIZONTAL_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    public PanePlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var block = updateState.currentBlock();
        var blockPosition = updateState.blockPosition();
        for (var blockFace : HORIZONTAL_FACES) {
            var neighbor = updateState.instance().getBlock(blockPosition.relative(blockFace));

            var canConnect = neighbor.isSolid() && !isFence(neighbor) && !isFenceGate(neighbor);
            block = block.withProperty(blockFace.name().toLowerCase(), String.valueOf(canConnect));
        }
        return block;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

    private boolean isFence(@NotNull Block block) {
        return BlockTags.MINECRAFT_FENCES.contains(block.namespace());
    }

    private boolean isFenceGate(@NotNull Block block) {
        return BlockTags.MINECRAFT_FENCE_GATES.contains(block.namespace());
    }

}
