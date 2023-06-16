package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SnowPlacementRule extends BlockPlacementRule {
    private static final String PROP_LAYERS = "layers";

    public SnowPlacementRule() {
        super(Block.SNOW);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition(), Block.Getter.Condition.TYPE);
        if (existingBlock.id() != Block.SNOW.id()) return block;

        var layers = Integer.parseInt(existingBlock.getProperty(PROP_LAYERS));
        // OK to always increment by 1, because isSelfReplaceable will prevent placing on a full block
        return placementState.block().withProperty(PROP_LAYERS, String.valueOf(layers + 1));
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Block block, @NotNull BlockFace blockFace, @NotNull Point cursorPosition) {
        return blockFace == BlockFace.TOP && block.id() == Block.SNOW.id() &&
                Integer.parseInt(block.getProperty(PROP_LAYERS)) < 8;
    }

}
