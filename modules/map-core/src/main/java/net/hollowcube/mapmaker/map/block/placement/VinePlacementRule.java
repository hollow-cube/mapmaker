package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static net.minestom.server.instance.block.Block.Getter.Condition.TYPE;

@SuppressWarnings("UnstableApiUsage")
public final class VinePlacementRule extends BaseBlockPlacementRule {
    private final boolean hasDown;

    public VinePlacementRule(@NotNull Block block, boolean hasDown) {
        super(block);
        this.hasDown = hasDown;
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        return computeState(
                placementState.instance(),
                placementState.placePosition(),
                placementState.block()
        );
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return computeState(
                updateState.instance(),
                updateState.blockPosition(),
                updateState.currentBlock()
        );
    }

    @Override
    public boolean isSelfReplaceable(@NotNull BlockPlacementRule.Replacement replacement) {
        return true;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    private @NotNull Block computeState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block current) {
        var props = new HashMap<String, String>();
        for (var face : BlockFace.values()) {
            if (!hasDown && face == BlockFace.BOTTOM) continue;

            var adjacentBlock = instance.getBlock(blockPosition.relative(face), TYPE);
            var direction = switch (face) {
                case BOTTOM -> "down";
                case TOP -> "up";
                default -> face.name().toLowerCase();
            };
            var value = adjacentBlock.isSolid() ? "true" : "false";
            props.put(direction, value);
        }

        return current.withProperties(props);
    }
}
