package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static net.minestom.server.instance.block.Block.Getter.Condition.TYPE;

@SuppressWarnings("UnstableApiUsage")
public final class ChorusPlantPlacementRule extends BaseBlockPlacementRule {
    private static final int PLANT_ID = Block.CHORUS_PLANT.id();
    private static final int FLOWER_ID = Block.CHORUS_FLOWER.id();

    public ChorusPlantPlacementRule(@NotNull Block block) {
        super(block);
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
    public int maxUpdateDistance() {
        return 1;
    }

    private @NotNull Block computeState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block current) {
        var props = new HashMap<String, String>();
        for (var face : BlockFace.values()) {
            var adjacentBlock = instance.getBlock(blockPosition.relative(face), TYPE);
            var direction = switch (face) {
                case BOTTOM -> "down";
                case TOP -> "up";
                default -> face.name().toLowerCase();
            };
            var value = adjacentBlock.id() == PLANT_ID || adjacentBlock.id() == FLOWER_ID ? "true" : "false";
            props.put(direction, value);
        }

        return current.withProperties(props);
    }
}
