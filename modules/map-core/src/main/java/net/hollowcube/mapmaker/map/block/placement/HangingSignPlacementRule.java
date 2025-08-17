package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HangingSignPlacementRule extends WaterloggedPlacementRule {

    private final Set<BlockFace> horizontalFaces = Set.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    public HangingSignPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placementState) {
        if (placementState.blockFace() == null) return null;

        final Block signBlock = switch (placementState.blockFace()) {
            case BOTTOM -> {
                // Attach to top, determine rotation
                float yaw = placementState.playerPosition().yaw() + 180;
                int rotation = (int) (Math.round(yaw / 22.5d) % 16);
                // https://minecraft.wiki/w/Hanging_Sign
                // The wiki lists as this should be set to true if
                // "If the block is too narrow, the chains meet together in a up-arrow shape. This version is also placed if the player is sneaking when placing a hanging sign below a wide-enough block. This version can be placed in sixteen different directions."
                // What counts as narrow enough? Beats me, I'll just put fences for the moment
                if (placementState.isPlayerShifting() || isBlockNarrowEnough(placementState.instance().getBlock(placementState.placePosition().relative(BlockFace.TOP))))
                    yield block.withProperty("rotation", String.valueOf(rotation)).withProperty("attached", "true");
                else {
                    // Technically, minecraft also restricts the rotation of this to 0, 4, 8, 12, but the game doesn't complain if it's set to other values
                    yield block.withProperty("rotation", String.valueOf(rotation)).withProperty("attached", "false");
                }
            }
            case TOP -> {
                // Convert block id to wall hanging sign id
                // It's because you always place as a ceiling hanging sign, because Minecraft?
                Block wallHangingSign = convertToWallSign(block);
                if (wallHangingSign == null) yield null;

                // Search for block to attach to
                List<BlockFace> validAttaches = new ArrayList<>();
                for (BlockFace face : horizontalFaces) {
                    if (placementState.instance().getBlock(placementState.placePosition().relative(face)).isSolid()) {
                        validAttaches.add(face);
                    }
                }
                if (validAttaches.isEmpty()) {
                    yield null;
                }
                // Prioritize the direction we are facing (sign faces opposite direction of player, but need a solid block to one of the
                BlockFace priority = BlockFace.fromYaw(placementState.playerPosition().yaw()).getOppositeFace();
                switch (priority) {
                    case EAST, WEST -> {
                        if (validAttaches.contains(BlockFace.NORTH) || validAttaches.contains(BlockFace.SOUTH)) {
                            yield wallHangingSign.withProperty("facing", priority.name().toLowerCase());
                        }
                        if (validAttaches.contains(BlockFace.EAST)) {
                            yield wallHangingSign.withProperty("facing", "north");
                        } else {
                            yield wallHangingSign.withProperty("facing", "south");
                        }
                    }
                    case NORTH, SOUTH -> {
                        if (validAttaches.contains(BlockFace.EAST) || validAttaches.contains(BlockFace.WEST)) {
                            yield wallHangingSign.withProperty("facing", priority.name().toLowerCase());
                        }
                        if (validAttaches.contains(BlockFace.SOUTH)) {
                            yield wallHangingSign.withProperty("facing", "west");
                        } else {
                            yield wallHangingSign.withProperty("facing", "east");
                        }
                    }
                    default -> {
                        yield null; // Should be unreachable
                    }
                }
            }
            case NORTH -> {
                Block wallHangingSign = convertToWallSign(block);
                if (wallHangingSign == null) yield null;

                yield wallHangingSign.withProperty("facing", "east");
            }
            case SOUTH -> {
                Block wallHangingSign = convertToWallSign(block);
                if (wallHangingSign == null) yield null;

                yield wallHangingSign.withProperty("facing", "west");
            }
            case WEST -> {
                Block wallHangingSign = convertToWallSign(block);
                if (wallHangingSign == null) yield null;

                yield wallHangingSign.withProperty("facing", "south");
            }
            case EAST -> {
                Block wallHangingSign = convertToWallSign(block);
                if (wallHangingSign == null) yield null;

                yield wallHangingSign.withProperty("facing", "north");
            }
        };
        return signBlock == null ? null : signBlock.withProperty("waterlogged", waterlogged(placementState));
    }

    private boolean isBlockNarrowEnough(@NotNull Block block) {
        return BlockTags.FENCES.contains(block.key());
    }

    @SuppressWarnings("PatternValidation")
    private @Nullable Block convertToWallSign(@NotNull Block block) {
        if (!BlockTags.CEILING_HANGING_SIGNS.contains(block.key()))
            throw new IllegalStateException("non hanging sign converted to hanging sign: " + block.key());

        var blockName = block.key().asString();
        int index = blockName.lastIndexOf('_', blockName.indexOf("hanging_sign"));
        if (index == -1) return null;

        var wallBlockName = blockName.substring(0, index + 1) + "wall_" + blockName.substring(index + 1);
        return Objects.requireNonNull(Block.fromKey(wallBlockName), wallBlockName)
                .withNbt(block.nbt()).withHandler(block.handler());
    }
}
