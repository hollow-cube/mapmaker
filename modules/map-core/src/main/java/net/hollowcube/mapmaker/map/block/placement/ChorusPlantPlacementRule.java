package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

import java.util.HashMap;

import static net.minestom.server.instance.block.Block.Getter.Condition.TYPE;

@SuppressWarnings("UnstableApiUsage")
public final class ChorusPlantPlacementRule extends BaseBlockPlacementRule {
    private static final int PLANT_ID = Block.CHORUS_PLANT.id();
    private static final int FLOWER_ID = Block.CHORUS_FLOWER.id();

    public ChorusPlantPlacementRule(Block block) {
        super(block);
    }

    @Override
    public Block blockPlace(PlacementState placementState) {
        return computeState(
                placementState.instance(),
                placementState.placePosition(),
                placementState.block()
        );
    }

    @Override
    public Block blockUpdate(UpdateState updateState) {
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

    private Block computeState(Block.Getter instance, Point blockPosition, Block current) {
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
