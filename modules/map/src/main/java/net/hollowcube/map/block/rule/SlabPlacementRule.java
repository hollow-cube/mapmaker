package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlabPlacementRule extends BlockPlacementRule {
    private static final String PROP_TYPE = "type";

    public SlabPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return updateState.currentBlock();
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var existingBlock = placementState.instance().getBlock(placementState.placePosition());
        if (existingBlock.id() == this.block.id()) {
            // This is already a slab, make it a double. isSelfReplaceable handles making sure its a valid click
            // to turn this into a double slab.
            return existingBlock.withProperty(PROP_TYPE, "double");
        }

        var blockFace = placementState.blockFace();
        if (blockFace == BlockFace.TOP) return block.withProperty(PROP_TYPE, "bottom");
        if (blockFace == BlockFace.BOTTOM) return block.withProperty(PROP_TYPE, "top");

        var type = placementState.cursorPosition().y() > 0.5 ? "top" : "bottom";
        return block.withProperty(PROP_TYPE, type);
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        var block = replacement.block();
        if (block.id() != this.block.id()) return false;

        var type = block.getProperty(PROP_TYPE);
        var blockFace = replacement.blockFace();
        if (blockFace == BlockFace.TOP || blockFace == BlockFace.BOTTOM)
            return !"double".equals(type);

        var cursorPosition = replacement.cursorPosition();
        return ("bottom".equals(type) && cursorPosition.y() > 0.5) ||
                ("top".equals(type) && cursorPosition.y() < 0.5);
    }
}
