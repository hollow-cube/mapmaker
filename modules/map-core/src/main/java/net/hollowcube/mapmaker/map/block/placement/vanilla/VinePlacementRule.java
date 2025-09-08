package net.hollowcube.mapmaker.map.block.placement.vanilla;

import net.hollowcube.mapmaker.map.block.placement.BaseBlockPlacementRule;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>NOT FOR PUBLIC DISTRIBUTION UNDER ANY CIRCUMSTANCE -- CODE IS DECOMPILED FROM THE VANILLA SERVER</p>
 *
 * <p>This placement rule is a copy paste of the vanilla logic for vines. In this case the function
 * {@link #getNearestLookingDirections(Pos, boolean, BlockFace)} is the problem. The client predicts
 * this ordering, and I wasnt able to recreate it perfectly on my own.</p>
 *
 * <p>At some point I will work out the math and rewrite this on my own, but for now it is fine.</p>
 *
 * <p>NOT FOR PUBLIC DISTRIBUTION UNDER ANY CIRCUMSTANCE -- CODE IS DECOMPILED FROM THE VANILLA SERVER</p>
 */
public final class VinePlacementRule extends BaseBlockPlacementRule {
    private final boolean hasDown;

    public VinePlacementRule(@NotNull Block block, boolean hasDown) {
        super(block);
        this.hasDown = hasDown;
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        var placeFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);

        var instance = placementState.instance();
        var blockPosition = placementState.placePosition();
        var existingBlock = instance.getBlock(blockPosition);

        return Arrays.stream(getNearestLookingDirections(playerPosition, existingBlock.id() == this.block.id(), placeFace))
                .filter(face -> face != BlockFace.BOTTOM || this.hasDown)
                .map(face -> this.getStateForPlacement(existingBlock, instance, blockPosition, face))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(existingBlock);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        // Do not update the state. We disagree with vanilla here.
        return updateState.currentBlock();
    }

    @Override
    public boolean isSelfReplaceable(@NotNull BlockPlacementRule.Replacement replacement) {
        return true;
    }

    private boolean isFaceSupported(BlockFace face) {
        return true;
    }


    public static boolean hasFace(Block block, BlockFace face) {
        return "true".equals(block.getProperty(getPropertyName(face)));
    }

    public static boolean canAttachTo(Block.Getter instance, BlockFace face, Point pos, Block block) {
        return !block.isAir() && block.id() != Block.GLOW_LICHEN.id() && block.id() != Block.VINE.id() && block.id() != Block.SCULK_VEIN.id();
//        return Block.isFaceFull($$3.getBlockSupportShape($$0, $$2), $$1.getOppositeFace()) || Block.isFaceFull($$3.getCollisionShape($$0, $$2), $$1.getOpposite());
    }

    public boolean isValidStateForPlacement(Block.Getter instance, Block block, Point pos, BlockFace face) {
        if (!this.isFaceSupported(face) || block.id() == this.block.id() && hasFace(block, face)) {
            return false;
        }
        Point posRel = pos.relative(face);
        return canAttachTo(instance, face, posRel, instance.getBlock(posRel, Block.Getter.Condition.TYPE));
    }

    @Nullable
    public Block getStateForPlacement(Block $$0, Block.Getter $$1, Point $$2, BlockFace $$3) {
        Block $$6;
        if (!this.isValidStateForPlacement($$1, $$0, $$2, $$3)) {
            return null;
        }
        if ($$0.id() == this.block.id()) {
            $$6 = $$0;
        } else if ($$0.id() == Block.WATER.id() && this.block.getProperty("waterlogged") != null) {
            $$6 = this.block.withProperty("waterlogged", "true");
        } else {
            $$6 = this.block;
        }
        return $$6.withProperty(getPropertyName($$3), "true");
    }

    private static @NotNull String getPropertyName(@NotNull BlockFace blockFace) {
        return switch (blockFace) {
            case BOTTOM -> "down";
            case TOP -> "up";
            default -> blockFace.name().toLowerCase(Locale.ROOT);
        };
    }

    public BlockFace[] getNearestLookingDirections(@NotNull Pos playerView, boolean replaceClicked, @NotNull BlockFace clickFace) {
        int $$2;
        BlockFace[] $$0 = orderedByNearest(playerView);
        if (replaceClicked) {
            return $$0;
        }
        BlockFace $$1 = clickFace;
        for ($$2 = 0; $$2 < $$0.length && $$0[$$2] != $$1.getOppositeFace(); ++$$2) {
        }
        if ($$2 > 0) {
            System.arraycopy($$0, 0, $$0, 1, $$2);
            $$0[0] = $$1.getOppositeFace();
        }
        return $$0;
    }

    public static BlockFace[] orderedByNearest(Pos $$0) {
        float $$1 = $$0.pitch() * ((float) Math.PI / 180);
        float $$2 = -$$0.yaw() * ((float) Math.PI / 180);
        float $$3 = (float) Math.sin($$1);
        float $$4 = (float) Math.cos($$1);
        float $$5 = (float) Math.sin($$2);
        float $$6 = (float) Math.cos($$2);
        boolean $$7 = $$5 > 0.0f;
        boolean $$8 = $$3 < 0.0f;
        boolean $$9 = $$6 > 0.0f;
        float $$10 = $$7 ? $$5 : -$$5;
        float $$11 = $$8 ? -$$3 : $$3;
        float $$12 = $$9 ? $$6 : -$$6;
        float $$13 = $$10 * $$4;
        float $$14 = $$12 * $$4;
        BlockFace $$15 = $$7 ? BlockFace.EAST : BlockFace.WEST;
        BlockFace $$16 = $$8 ? BlockFace.TOP : BlockFace.BOTTOM;
        BlockFace $$17 = $$9 ? BlockFace.SOUTH : BlockFace.NORTH;
        if ($$10 > $$12) {
            if ($$11 > $$13) {
                return makeDirectionArray($$16, $$15, $$17);
            }
            if ($$14 > $$11) {
                return makeDirectionArray($$15, $$17, $$16);
            }
            return makeDirectionArray($$15, $$16, $$17);
        }
        if ($$11 > $$14) {
            return makeDirectionArray($$16, $$17, $$15);
        }
        if ($$13 > $$11) {
            return makeDirectionArray($$17, $$15, $$16);
        }
        return makeDirectionArray($$17, $$16, $$15);
    }

    private static BlockFace[] makeDirectionArray(BlockFace $$0, BlockFace $$1, BlockFace $$2) {
        return new BlockFace[]{$$0, $$1, $$2, $$2.getOppositeFace(), $$1.getOppositeFace(), $$0.getOppositeFace()};
    }

}
