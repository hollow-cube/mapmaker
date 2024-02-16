package net.hollowcube.map2.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.block.BlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChestPlacementRule extends BaseBlockPlacementRule {

    public ChestPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        Block currBlock = updateState.currentBlock();
        String facingDirection = currBlock.getProperty("facing");
        BlockUtils blockUtils = new BlockUtils(updateState.instance(), updateState.blockPosition());
        if (currBlock.getProperty("type").equals("single")) {
            // Check for connected chests
            switch (facingDirection) {
                case "north" -> {
                    Block westBlock = blockUtils.west().getBlock();
                    Block eastBlock = blockUtils.east().getBlock();
                    if (westBlock.compare(currBlock) && westBlock.getProperty("facing").equals(facingDirection) && westBlock.getProperty("type").equals("left")) {
                        return currBlock.withProperty("type", "right");
                    }
                    if (eastBlock.compare(currBlock) && eastBlock.getProperty("facing").equals(facingDirection) && eastBlock.getProperty("type").equals("right")) {
                        return currBlock.withProperty("type", "left");
                    }
                }
                case "south" -> {
                    Block westBlock = blockUtils.west().getBlock();
                    Block eastBlock = blockUtils.east().getBlock();
                    if (westBlock.compare(currBlock) && westBlock.getProperty("facing").equals(facingDirection) && westBlock.getProperty("type").equals("right")) {
                        return currBlock.withProperty("type", "left");
                    }
                    if (eastBlock.compare(currBlock) && eastBlock.getProperty("facing").equals(facingDirection) && eastBlock.getProperty("type").equals("left")) {
                        return currBlock.withProperty("type", "right");
                    }
                }
                case "east" -> {
                    Block northBlock = blockUtils.north().getBlock();
                    Block southBlock = blockUtils.south().getBlock();
                    if (northBlock.compare(currBlock) && northBlock.getProperty("facing").equals(facingDirection) && northBlock.getProperty("type").equals("left")) {
                        return currBlock.withProperty("type", "right");
                    }
                    if (southBlock.compare(currBlock) && southBlock.getProperty("facing").equals(facingDirection) && southBlock.getProperty("type").equals("right")) {
                        return currBlock.withProperty("type", "left");
                    }
                }
                case "west" -> {
                    Block northBlock = blockUtils.north().getBlock();
                    Block southBlock = blockUtils.south().getBlock();
                    if (northBlock.compare(currBlock) && northBlock.getProperty("facing").equals(facingDirection) && northBlock.getProperty("type").equals("right")) {
                        return currBlock.withProperty("type", "left");
                    }
                    if (southBlock.compare(currBlock) && southBlock.getProperty("facing").equals(facingDirection) && southBlock.getProperty("type").equals("left")) {
                        return currBlock.withProperty("type", "right");
                    }
                }
            }
        } else {
            switch (facingDirection) {
                case "north" -> {
                    if (currBlock.getProperty("type").equals("left")) {
                        if (!blockUtils.east().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    } else if (currBlock.getProperty("type").equals("right")) {
                        if (!blockUtils.west().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    }
                }
                case "south" -> {
                    if (currBlock.getProperty("type").equals("left")) {
                        if (!blockUtils.west().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    } else if (currBlock.getProperty("type").equals("right")) {
                        if (!blockUtils.east().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    }
                }
                case "east" -> {
                    if (currBlock.getProperty("type").equals("left")) {
                        if (!blockUtils.south().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    } else if (currBlock.getProperty("type").equals("right")) {
                        if (!blockUtils.north().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    }
                }
                case "west" -> {
                    if (currBlock.getProperty("type").equals("left")) {
                        if (!blockUtils.north().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    } else if (currBlock.getProperty("type").equals("right")) {
                        if (!blockUtils.south().getBlock().compare(currBlock)) {
                            return currBlock.withProperty("type", "single");
                        }
                    }
                }
            }
        }
        return currBlock;
    }

    //       North: -Z
    // West: -X      East: +X
    //       South: +Z

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        Block currBlock = super.block;
        switch (placementState.blockFace()) {
            case TOP, BOTTOM -> {
                float yaw = placementState.playerPosition().yaw();
                if (yaw >= 135 || yaw <= -135) {
                    currBlock = currBlock.withProperty("facing", "south");
                } else if (yaw >= 45) {
                    currBlock = currBlock.withProperty("facing", "east");
                } else if (yaw >= -45) {
                    currBlock = currBlock.withProperty("facing", "north");
                } else {
                    currBlock = currBlock.withProperty("facing", "west");
                }
            }
            case EAST -> currBlock = currBlock.withProperty("facing", "west");
            case SOUTH -> currBlock = currBlock.withProperty("facing", "north");
            case NORTH -> currBlock = currBlock.withProperty("facing", "south");
            case WEST -> currBlock = currBlock.withProperty("facing", "east");
        }
        if (!placementState.isPlayerShifting()) {
            String facingDirection = currBlock.getProperty("facing");
            BlockUtils blockUtils = new BlockUtils(placementState.instance(), placementState.placePosition());
            // Check and try to make double chests
            // To combine into a double chest, it must be facing the same way on the left or right sides of the chest and must be the same type of chest
            switch (facingDirection) {
                case "south", "north" -> {
                    // Check blocks +/- X direction
                    Block east = blockUtils.east().getBlock();
                    Block west = blockUtils.west().getBlock();
                    if (east.compare(block) && east.getProperty("facing").equals(facingDirection) && east.getProperty("type").equals("single")) {
                        // Valid chest, update self, neighbor update resolved in blockUpdate()
                        if (facingDirection.equals("south")) {
                            return currBlock.withProperty("type", "right");
                        } else {
                            return currBlock.withProperty("type", "left");
                        }
                    }
                    if (west.compare(block) && west.getProperty("facing").equals(facingDirection) && west.getProperty("type").equals("single")) {
                        // Valid chest, update self, neighbor update resolved in blockUpdate()
                        if (facingDirection.equals("south")) {
                            return currBlock.withProperty("type", "left");
                        } else {
                            return currBlock.withProperty("type", "right");
                        }
                    }
                }
                case "east", "west" -> {
                    // Check blocks +/- Z direction
                    Block south = blockUtils.south().getBlock();
                    Block north = blockUtils.north().getBlock();
                    if (south.compare(block) && south.getProperty("facing").equals(facingDirection) && south.getProperty("type").equals("single")) {
                        // Valid chest, update self, neighbor update resolved in blockUpdate()
                        if (facingDirection.equals("west")) {
                            return currBlock.withProperty("type", "right");
                        } else {
                            return currBlock.withProperty("type", "left");
                        }
                    }
                    if (north.compare(block) && north.getProperty("facing").equals(facingDirection) && north.getProperty("type").equals("single")) {
                        // Valid chest, update self, neighbor update resolved in blockUpdate()
                        if (facingDirection.equals("west")) {
                            return currBlock.withProperty("type", "left");
                        } else {
                            return currBlock.withProperty("type", "right");
                        }
                    }
                }
            }
        } else {
            return currBlock.withProperty("type", "single");
        }
        return currBlock;
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

}
