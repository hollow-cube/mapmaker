package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.hollowcube.mapmaker.map.block.placement.FacingClickHorizontalPlacementRule.getImplicitFace;

public class CoralFanPlacementRule extends BlockPlacementRule {
    private final Block wallBlock;

    public CoralFanPlacementRule(@NotNull Block block) {
        super(block);

        var wallBlockId = block.key().asString().replace("fan", "wall_fan");
        this.wallBlock = Objects.requireNonNull(Block.fromKey(wallBlockId));
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placement) {
        var instance = placement.instance();
        var blockPosition = placement.placePosition();

        var blockFace = Objects.requireNonNullElse(placement.blockFace(), BlockFace.TOP);
        var baseBlock = switch (blockFace) {
            case TOP -> this.block;
            case NORTH, SOUTH, EAST, WEST -> this.wallBlock.withProperty("facing", blockFace.name().toLowerCase());
            case BOTTOM -> {
                var implicitFace = getImplicitFace(placement);
                if (implicitFace != null)
                    yield this.wallBlock.withProperty("facing", implicitFace.name().toLowerCase());

                // Try the bottom face
                var belowBlock = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM), Block.Getter.Condition.TYPE);
                if (belowBlock.isSolid())
                    yield this.block;

                // Nothing, cannot place
                yield null;
            }
        };
        if (baseBlock == null)
            return null;

        var existing = instance.getBlock(blockPosition, Block.Getter.Condition.TYPE);
        return baseBlock.withProperty("waterlogged", String.valueOf(existing.id() == Block.WATER.id()));
    }

}
