package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;

public class RailCurvedPlacementRule extends BaseBlockPlacementRule {
    public RailCurvedPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placement) {
        var instance = placement.instance();
        var blockPosition = placement.placePosition();
//        return computePlaceState(instance, blockPosition);
        var newState = this.updateDir(instance, blockPosition, block, true);
//        if (this.isStraight) {
//            $$1.neighborChanged(newState, $$2, this, $$2, $$3);
//        }
        return newState;

    }


    protected Block updateDir(Block.Getter $$0, Point $$1, Block $$2, boolean $$3) {
        var shape = $$2.getProperty("shape");
        return new RailStuff.RailState($$0, $$1, $$2).place(true, $$3, shape).getState();
    }

    // It is valid to update the shape IF
    // 0. the block on fromFace is a rail pointing into the update direction.
    // 1. one of the ends is air
    // 2. one of the ends is a rail
    //   a. one of the ends of that rail is not a rail

    // we MUST update the shape from the side which updated us

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState update) {
        var instance = update.instance();
        var blockPosition = update.blockPosition();
//        return this.updateDir(instance, blockPosition, block, true);

        return update.currentBlock();


        // The block updating us must be a rail, and it must face into us.
//        var fromFace = update.fromFace();
//        var fromBlock = instance.getBlock(blockPosition.relative(fromFace), Block.Getter.Condition.TYPE);
//        if (!isRail(fromBlock)) // || !getShapeFaces(fromBlock).contains(fromFace.getOppositeFace())
//            return update.currentBlock();
//
//        // If one of our ends is not a rail then we should always update (there is an opening to reset our position)
//        var currentShape = getShapeFaces(update.currentBlock());
//        for (var face : currentShape) {
//            var neighborPosition = blockPosition.relative(face);
//            var neighborBlock = instance.getBlock(neighborPosition, Block.Getter.Condition.TYPE);
//            if (!isRail(neighborBlock))
//                return computeStateFrom(instance, blockPosition, fromFace); //todo not quite, we MUST prioritize the face we updated from
//
//
//            // It can still be valid to update if the rail is connected on both sides (meaning we could never connect to them)
//            boolean hasSideConnections = true;
//            for (var neighborFace : getShapeFaces(neighborBlock)) {
//                if (neighborFace.getOppositeFace() == face) continue; // Skip the face we are connected to
//                if (isRail(instance.getBlock(neighborPosition.relative(neighborFace), Block.Getter.Condition.TYPE)))
//                    continue; // The neighbor has a rail connection
//
//                hasSideConnections = false;
//            }
//
//            if (!hasSideConnections) {
//                // === RECOMPUTE STATE ===
//                return computeStateFrom(instance, blockPosition, fromFace);
//            }
//        }
//
//        return update.currentBlock();
        // END MAYBE


        // At this point we must be pointing into two rails on either side.


//        return this.updateDir(instance, blockPosition, block, true);
//
//        // If the fromBlock is air it means it was just removed, so don't do an update.
//        var fromBlock = instance.getBlock(blockPosition.relative(update.fromFace()));
//        if (fromBlock.isAir()) return update.currentBlock();
//
//        // If the current shape has both ends connected then we should never update it.
//        int shapeConnections = countShapeFacesPlace(instance, blockPosition, update.currentBlock(), false);
//        if (shapeConnections >= 2) return update.currentBlock();
//
//        //todo when updating we should prioritize the update from face. If that has an open connection we should always connect to it.
//
//        return computeState(instance, blockPosition);
    }

    private @NotNull Block computeStateFrom(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace fromFace) {
        // This function should compute a new state, but the state MUST include fromFace.

        // We are already being called from an update, meaning no more updates can be triggered. Because of this, we


        int north = fromFace == BlockFace.NORTH ? LEVEL : getConnectionState54242(instance, blockPosition, BlockFace.NORTH, true);
        int south = fromFace == BlockFace.SOUTH ? LEVEL : getConnectionState54242(instance, blockPosition, BlockFace.SOUTH, true);
        int east = fromFace == BlockFace.EAST ? LEVEL : getConnectionState54242(instance, blockPosition, BlockFace.EAST, true);
        int west = fromFace == BlockFace.WEST ? LEVEL : getConnectionState54242(instance, blockPosition, BlockFace.WEST, true);

        System.out.println("bp: " + blockPosition + " north: " + north + " south: " + south + " east: " + east + " west: " + west);

//        int nRails = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
        int mask = (north != NONE ? 0x8 : 0) | (south != NONE ? 0x4 : 0) | (east != NONE ? 0x2 : 0) | (west != NONE ? 0x1 : 0);
        return block.withProperty("shape", HORIZONTAL_SHAPES[mask]);


    }

    private @NotNull Block stateFromFaces(@NotNull Block current, @NotNull BlockFace first, @NotNull BlockFace second) {
        int mask = (first == BlockFace.NORTH || second == BlockFace.NORTH ? 0x8 : 0)
                | (first == BlockFace.SOUTH || second == BlockFace.SOUTH ? 0x4 : 0)
                | (first == BlockFace.EAST || second == BlockFace.EAST ? 0x2 : 0)
                | (first == BlockFace.WEST || second == BlockFace.WEST ? 0x1 : 0);
        return current.withProperty("shape", HORIZONTAL_SHAPES[mask]);
    }

    @Override
    public int maxUpdateDistance() {
        return 0;
    }

    private @NotNull Block computeState(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
        int north = getConnectionState(instance, blockPosition, BlockFace.NORTH, false);
        int south = getConnectionState(instance, blockPosition, BlockFace.SOUTH, false);
        int east = getConnectionState(instance, blockPosition, BlockFace.EAST, false);
        int west = getConnectionState(instance, blockPosition, BlockFace.WEST, false);

//        int nRails = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
        int mask = (north != NONE ? 0x8 : 0) | (south != NONE ? 0x4 : 0) | (east != NONE ? 0x2 : 0) | (west != NONE ? 0x1 : 0);
        return block.withProperty("shape", HORIZONTAL_SHAPES[mask]);
    }

    private @NotNull Block computePlaceState(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
        int north = getConnectionStatePlace(instance, blockPosition, BlockFace.NORTH, false);
        int south = getConnectionStatePlace(instance, blockPosition, BlockFace.SOUTH, false);
        int east = getConnectionStatePlace(instance, blockPosition, BlockFace.EAST, false);
        int west = getConnectionStatePlace(instance, blockPosition, BlockFace.WEST, false);

//        int nRails = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
        int mask = (north != NONE ? 0x8 : 0) | (south != NONE ? 0x4 : 0) | (east != NONE ? 0x2 : 0) | (west != NONE ? 0x1 : 0);
        return block.withProperty("shape", HORIZONTAL_SHAPES[mask]);
    }

    private int countShapeFaces(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block block, boolean naive) {
        int count = 0;
        for (var face : SHAPE_FACE_MAP.get(block.getProperty("shape"))) {
            if (getConnectionState(instance, blockPosition, face, naive) != NONE)
                count++;
        }
        return count;
    }

    private BlockFace[] getNeighborConnectionOptions(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace face) {
        blockPosition = blockPosition.relative(face);
        for (int y = -1; y <= 1; y++) {
            var targetPosition = blockPosition.add(0, y, 0);
            var targetBlock = instance.getBlock(targetPosition);
            if (!isRail(targetBlock)) continue;

            // For this connection to be valid the target rail must also not already have 2 other connections.
//            if (!naive && countShapeFaces(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countNeighborsNaive(instance, targetPosition) > 1) return NONE;

            return SHAPE_FACE_MAP.get(targetBlock.getProperty("shape"));
        }
        return null;
    }

    private int countShapeFacesPlace(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull Block block, boolean naive) {
        int count = 0;
        for (var face : SHAPE_FACE_MAP.get(block.getProperty("shape"))) {
            // 1. Find the rail being connected (if there is one)
            var neighborFaces = getNeighborConnectionOptions(instance, blockPosition, face);
            if (neighborFaces == null) continue;

            // 2. This can only be a valid connection if the neighbor connects to this face
            if (neighborFaces[0] != face.getOppositeFace() && neighborFaces[1] != face.getOppositeFace()) continue;

            count++;
        }
        return count;
    }

    private static final int NONE = 0; // There is no rail
    private static final int LEVEL = 1; // There is a rail at the same level (or below)
    private static final int INCLINE = 2; // There is a rail one block up

    // Returns a connection state for the given direction at the given blockPosition.
    // Basically checks all 3 blocks in a vertical column
    private int getConnectionState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace face, boolean naive) {
        blockPosition = blockPosition.relative(face);
        for (int y = -1; y <= 1; y++) {
            var targetPosition = blockPosition.add(0, y, 0);
            var targetBlock = instance.getBlock(targetPosition);
            if (!isRail(targetBlock)) continue;

            // For this connection to be valid the target rail must also not already have 2 other connections.
            if (!naive && countShapeFacesPlace(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countShapeFaces(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countNeighborsNaive(instance, targetPosition) > 1) return NONE;

            return y == 1 ? INCLINE : LEVEL;
        }
        return NONE;
    }

    private int getConnectionState54242(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace face, boolean naive) {
        blockPosition = blockPosition.relative(face);
        for (int y = -1; y <= 1; y++) {
            var targetPosition = blockPosition.add(0, y, 0);
            var targetBlock = instance.getBlock(targetPosition);
            if (!isRail(targetBlock)) continue;

            if (!getShapeFaces(targetBlock).contains(face.getOppositeFace())) continue;


            // For this connection to be valid the target rail must also not already have 2 other connections.
//            if (!naive && countShapeFacesPlace(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countShapeFaces(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countNeighborsNaive(instance, targetPosition) > 1) return NONE;


            return y == 1 ? INCLINE : LEVEL;
        }
        return NONE;
    }


    private int getConnectionStatePlace(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace face, boolean naive) {
        blockPosition = blockPosition.relative(face);
        for (int y = -1; y <= 1; y++) {
            var targetPosition = blockPosition.add(0, y, 0);
            var targetBlock = instance.getBlock(targetPosition);
            if (!isRail(targetBlock)) continue;

            // For this connection to be valid the target rail must also not already have 2 other connections.
            if (!naive && countShapeFacesPlace(instance, targetPosition, targetBlock, true) > 1) return NONE;
//            if (!naive && countNeighborsNaive(instance, targetPosition) > 1) return NONE;

            return y == 1 ? INCLINE : LEVEL;
        }
        return NONE;
    }

    private int countNeighborsNaive(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
        int north = getConnectionState(instance, blockPosition, BlockFace.NORTH, true);
        int south = getConnectionState(instance, blockPosition, BlockFace.SOUTH, true);
        int east = getConnectionState(instance, blockPosition, BlockFace.EAST, true);
        int west = getConnectionState(instance, blockPosition, BlockFace.WEST, true);
        return (north != NONE ? 1 : 0) + (south != NONE ? 1 : 0) + (east != NONE ? 1 : 0) + (west != NONE ? 1 : 0);
    }

    private static boolean isRail(@NotNull Block block) {
        return block.id() == Block.RAIL.id() || block.id() == Block.POWERED_RAIL.id() ||
                block.id() == Block.DETECTOR_RAIL.id() || block.id() == Block.ACTIVATOR_RAIL.id();
    }

    private static @NotNull EnumSet<BlockFace> getShapeFaces(@NotNull Block block) {
        var faces = SHAPE_FACE_MAP.get(block.getProperty("shape"));
        return EnumSet.of(faces[0], faces[1]);
    }

    private static final String[] HORIZONTAL_SHAPES = new String[]{
            //                 NSEW
            "north_south",             // 0000 todo depends on player direction
            "east_west",    // 0001
            "east_west",    // 0010
            "east_west",    // 0011
            "north_south",  // 0100
            "south_west",   // 0101
            "south_east",   // 0110
            "south_east",   // 0111
            "north_south",  // 1000
            "north_west",   // 1001
            "north_east",   // 1010
            "north_east",   // 1011
            "north_south",  // 1100
            "south_west",   // 1101
            "south_east",   // 1110
            "south_east",   // 1111
    };

    private static final Map<String, BlockFace[]> SHAPE_FACE_MAP = Map.ofEntries(
            Map.entry("east_west", new BlockFace[]{BlockFace.EAST, BlockFace.WEST}),
            Map.entry("north_east", new BlockFace[]{BlockFace.NORTH, BlockFace.EAST}),
            Map.entry("north_south", new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH}),
            Map.entry("north_west", new BlockFace[]{BlockFace.NORTH, BlockFace.WEST}),
            Map.entry("south_east", new BlockFace[]{BlockFace.SOUTH, BlockFace.EAST}),
            Map.entry("south_west", new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST})
    );


    //    private Block calculateBlockState(@NotNull Instance instance, @NotNull Block block, @NotNull Point blockPosition, @Nullable Player player) {
    //        boolean north = isRailInDirection(instance, blockPosition, Direction.NORTH);
    //        boolean south = isRailInDirection(instance, blockPosition, Direction.SOUTH);
    //        boolean east = isRailInDirection(instance, blockPosition, Direction.EAST);
    //        boolean west = isRailInDirection(instance, blockPosition, Direction.WEST);
    //
    //        int nRails = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
    //        int mask = (north ? 0x8 : 0) | (south ? 0x4 : 0) | (east ? 0x2 : 0) | (west ? 0x1 : 0);
    //
    //        // Do not update from neighbors unless there are exactly 2 neighbors
    //        if (player == null && nRails != 2)
    //            return block;
    //
    //        if (player != null && mask == 0) {
    //            Direction direction = getHorizontalFacingDirection(player);
    //            return Block.RAIL.withProperty("shape", direction == Direction.NORTH || direction == Direction.SOUTH ? "north_south" : "east_west");
    //        }
    //
    //        return Block.RAIL.withProperty("shape", HORIZONTAL_SHAPES[mask]);
    //
    //    }
    //
    //
    //    private boolean isRailInDirection(@NotNull Instance instance, @NotNull Point blockPosition, @NotNull Direction direction) {
    //        return instance.getBlock(blockPosition.add(direction.normalX(), direction.normalY(), direction.normalZ())).id() == Block.RAIL.id();
    //    }
    //
    //    private Direction getHorizontalFacingDirection(@NotNull Player player) {
    //        float yaw = player.getPosition().yaw() + 180;
    //        // Get NSEW from yaw
    //        if (yaw >= 45 && yaw < 135) {
    //            return Direction.EAST;
    //        } else if (yaw >= 135 && yaw < 225) {
    //            return Direction.NORTH;
    //        } else if (yaw >= 225 && yaw < 315) {
    //            return Direction.WEST;
    //        } else {
    //            return Direction.SOUTH;
    //        }
    //    }
}
