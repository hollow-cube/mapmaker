package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;

public final class PaleMossCarpetPlacementRule extends BaseBlockPlacementRule {

    public PaleMossCarpetPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        return computeState(placementState.instance(), placementState.placePosition(), this.block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        return computeState(updateState.instance(), updateState.blockPosition(), updateState.currentBlock());
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    private @NotNull Block computeState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block block) {
        var above = instance.getBlock(blockPosition.relative(BlockFace.TOP), Block.Getter.Condition.TYPE);
        boolean isAboveCarpet = above.id() == Block.PALE_MOSS_CARPET.id();

        var isAnyAttached = false;
        var newProperties = new HashMap<String, String>();
        for (var face : HORIZONTAL) {
            var adjacent = instance.getBlock(blockPosition.relative(face), Block.Getter.Condition.TYPE);
            var isLow = adjacent.registry().collisionShape().isFaceFull(face.getOppositeFace());
            isAnyAttached |= isLow;
            if (!isLow) {
                newProperties.put(face.name().toLowerCase(Locale.ROOT), "none");
                continue;
            }

            if (isAboveCarpet) {
                var aboveSide = instance.getBlock(blockPosition.relative(face).relative(BlockFace.TOP), Block.Getter.Condition.TYPE);
                if (aboveSide.registry().collisionShape().isFaceFull(face.getOppositeFace())) {
                    newProperties.put(face.name().toLowerCase(Locale.ROOT), "tall");
                    continue;
                }
            }

            newProperties.put(face.name().toLowerCase(Locale.ROOT), "low");
        }

        var below = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM), Block.Getter.Condition.TYPE);
        var hasBottomFace = below.registry().collisionShape().isFaceFull(BlockFace.TOP);
        if (!hasBottomFace && !isAnyAttached) return Block.AIR;

        newProperties.put("bottom", hasBottomFace ? "true" : "false");

        return block.withProperties(newProperties);
    }
}
