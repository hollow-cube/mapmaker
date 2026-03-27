package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StructureVoidPlacementRule extends BlockPlacementRule {

    public StructureVoidPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

    @Override
    public boolean isSelfReplaceable(@NotNull Replacement replacement) {
        return replacement.material() != Material.STRUCTURE_VOID;
    }
}
