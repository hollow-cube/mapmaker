package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TripwirePlacementRule extends BaseBlockPlacementRule {
    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    public TripwirePlacementRule() {
        super(Block.TRIPWIRE);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var block = updateState.currentBlock();
        var blockPosition = updateState.blockPosition();
        for (var blockFace : HORIZONTAL_FACES) {
            var neighbor = updateState.instance().getBlock(blockPosition.relative(blockFace));

            var canConnect = neighbor.id() == Block.TRIPWIRE.id() || neighbor.id() == Block.TRIPWIRE_HOOK.id();
            block = block.withProperty(blockFace.name().toLowerCase(), String.valueOf(canConnect));
        }

        return block;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

}
