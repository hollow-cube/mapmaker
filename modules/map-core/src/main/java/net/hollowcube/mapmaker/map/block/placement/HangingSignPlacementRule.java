package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HangingSignPlacementRule extends BaseBlockPlacementRule {

    private final Set<BlockFace> horizontalFaces = Set.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    public HangingSignPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placementState) {
        if (placementState.blockFace() == null) return null;

        return switch (placementState.blockFace()) {
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
    }

    private boolean isBlockNarrowEnough(@NotNull Block block) {
        return BlockTags.FENCES.contains(block.namespace());
    }

    private @Nullable Block convertToWallSign(@NotNull Block block) {
        if (!BlockTags.CEILING_HANGING_SIGNS.contains(block.namespace())) {
            System.out.println("Tried to convert a non-hanging sign into a wall hanging sign, " + block.namespace());
            return null;
        }
        // TODO Is there a better way?
        // Something like indexof('_'), then do substring(0, indexof('_')) + "wall_" + substring(indexof('_')) ?
        if (block.compare(Block.OAK_HANGING_SIGN)) {
            return Block.OAK_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.DARK_OAK_HANGING_SIGN)) {
            return Block.DARK_OAK_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.BIRCH_HANGING_SIGN)) {
            return Block.BIRCH_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.JUNGLE_HANGING_SIGN)) {
            return Block.JUNGLE_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.ACACIA_HANGING_SIGN)) {
            return Block.ACACIA_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.SPRUCE_HANGING_SIGN)) {
            return Block.SPRUCE_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.MANGROVE_HANGING_SIGN)) {
            return Block.MANGROVE_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.CHERRY_HANGING_SIGN)) {
            return Block.CHERRY_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.BAMBOO_HANGING_SIGN)) {
            return Block.BAMBOO_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.CRIMSON_HANGING_SIGN)) {
            return Block.CRIMSON_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else if (block.compare(Block.WARPED_HANGING_SIGN)) {
            return Block.WARPED_WALL_HANGING_SIGN.withNbt(block.nbt()).withHandler(block.handler());
        } else {
            return null;
        }
    }
}
