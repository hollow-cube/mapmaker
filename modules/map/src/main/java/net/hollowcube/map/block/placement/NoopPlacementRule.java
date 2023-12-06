package net.hollowcube.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exists to handle placement for some block handlers
 */
public class NoopPlacementRule extends BlockPlacementRule {

    public NoopPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        return block;
    }

}
