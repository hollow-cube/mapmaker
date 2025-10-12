package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.common.util.PropertyUtil;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.schem.Rotation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class ShelfPlacementRule extends FacingHorizontalPlacementRule {

    public ShelfPlacementRule(@NotNull Block block) {
        super(block, true);
    }

    @Override
    public @UnknownNullability Block blockPlace(@NotNull PlacementState state) {
        return genericUpdateState(state.instance(), super.blockPlace(state), state.placePosition());
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return genericUpdateState(updateState.instance(), updateState.currentBlock(), updateState.blockPosition());
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    private @NotNull Block genericUpdateState(@NotNull Block.Getter instance, @NotNull Block block, @NotNull Point blockPosition) {
        var direction = PropertyUtil.getFacing(block.properties());
        if (direction == null || direction.vertical()) return block;

        var leftDir = Rotation.CLOCKWISE_90.rotate(direction);
        var rightDir = Rotation.CLOCKWISE_270.rotate(direction);

        var left = instance.getBlock(blockPosition.add(leftDir.normalX(), leftDir.normalY(), leftDir.normalZ()));
        var right = instance.getBlock(blockPosition.add(rightDir.normalX(), rightDir.normalY(), rightDir.normalZ()));

        var leftIsShelf = BlockTags.SHELVES.contains(left.key());
        var rightIsShelf = BlockTags.SHELVES.contains(right.key());

        if (leftIsShelf && rightIsShelf) {
            block = block.withProperty("side_chain", "center");
        } else if (leftIsShelf) {
            block = block.withProperty("side_chain", "right");
        } else if (rightIsShelf) {
            block = block.withProperty("side_chain", "left");
        } else {
            block = block.withProperty("side_chain", "unconnected");
        }

        return block;
    }
}
