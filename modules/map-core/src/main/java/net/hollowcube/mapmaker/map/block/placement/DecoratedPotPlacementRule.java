package net.hollowcube.mapmaker.map.block.placement;


import net.hollowcube.mapmaker.map.block.handler.DecoratedPotBlockHandler;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import org.jetbrains.annotations.NotNull;

public class DecoratedPotPlacementRule extends FacingHorizontalPlacementRule {

    public DecoratedPotPlacementRule(@NotNull Block block) {
        super(block, false);
    }

    @Override
    public Block blockPlace(@NotNull PlacementState placementState) {
        var sherds = placementState.usedItemStack() == null ? null : placementState.usedItemStack().get(ItemComponent.POT_DECORATIONS);
        return super.blockPlace(placementState).withTag(DecoratedPotBlockHandler.SHERDS, sherds);
    }

}
