package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class WallPlacementRule extends BaseBlockPlacementRule {
    private static final BlockFace[] HORIZONTAL_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
    };

    //todo it should not connect to fence gates

    public WallPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var instance = updateState.instance();
        var blockPosition = updateState.blockPosition();
        var updateFace = updateState.fromFace();
        var block = updateState.currentBlock();

        var above = instance.getBlock(blockPosition.add(0, 1, 0), Block.Getter.Condition.TYPE);

        if (updateFace == BlockFace.TOP || updateFace == BlockFace.BOTTOM) {
            var north = getConnectionState(instance, blockPosition, BlockFace.NORTH, above);
            var south = getConnectionState(instance, blockPosition, BlockFace.SOUTH, above);
            var east = getConnectionState(instance, blockPosition, BlockFace.EAST, above);
            var west = getConnectionState(instance, blockPosition, BlockFace.WEST, above);

            var up = String.valueOf(isUpright(above, north, south, east, west));

            return block.withProperties(Map.of(
                    "east", east,
                    "north", north,
                    "south", south,
                    "west", west,
                    "up", up
            ));
        } else {

            var newFaceValue = getConnectionState(instance, blockPosition, updateFace, above);

            var faces = new String[4];
            for (var face : HORIZONTAL_FACES) {
                if (face == updateFace) {
                    faces[face.ordinal() - 2] = newFaceValue;
                } else {
                    faces[face.ordinal() - 2] = block.getProperty(face.toString().toLowerCase());
                }
            }

            var up = String.valueOf(isUpright(above, faces[0], faces[1], faces[2], faces[3]));

            return block.withProperties(Map.of(
                    updateFace.name().toLowerCase(), newFaceValue,
                    "up", up
            ));
        }
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        var instance = placementState.instance();
        var blockPosition = placementState.placePosition();

        var above = instance.getBlock(blockPosition.add(0, 1, 0), Block.Getter.Condition.TYPE);

        var north = getConnectionState(instance, blockPosition, BlockFace.NORTH, above);
        var south = getConnectionState(instance, blockPosition, BlockFace.SOUTH, above);
        var east = getConnectionState(instance, blockPosition, BlockFace.EAST, above);
        var west = getConnectionState(instance, blockPosition, BlockFace.WEST, above);

        var up = String.valueOf(isUpright(above, north, south, east, west));

        return block.withProperties(Map.of(
                "east", east,
                "north", north,
                "south", south,
                "west", west,
                "up", up
        ));
    }

    @Override
    public int maxUpdateDistance() {
        return 2;
    }

    private @NotNull String getConnectionState(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace blockFace, @NotNull Block aboveBlock) {
        var neighbor = instance.getBlock(blockPosition.relative(blockFace), Block.Getter.Condition.TYPE);
        var neighborFaceIsSolid = neighbor.registry().collisionShape().isOccluded(block.registry().collisionShape(), blockFace.getOppositeFace());
        var canConnect = canConnect(neighbor) && (neighborFaceIsSolid || isConnectable(neighbor)
                || FenceGatePlacementRule.isConnectableGate(neighbor, blockFace));
        if (!canConnect) return "none";
        return aboveBlock.isSolid() && hasFacingConnection(aboveBlock, blockFace) ? "tall" : "low";
    }

    private boolean hasFacingConnection(@NotNull Block block, @NotNull BlockFace dir) {
        // This pretends that anything without a side property is a connection
        var value = block.getProperty(dir.name().toLowerCase());
        return !("false".equals(value) || "none".equals(value));
    }

    private boolean isUpright(@NotNull Block aboveBlock, @NotNull String north, @NotNull String south, @NotNull String east, @NotNull String west) {
        if ("true".equals(aboveBlock.getProperty("up"))) return true;

        boolean hasNorth = !north.equals("none"), hasSouth = !south.equals("none");
        boolean hasEast = !east.equals("none"), hasWest = !west.equals("none");

        return !((hasNorth && hasSouth && !hasEast && !hasWest)   // straight north-south
                || (!hasNorth && !hasSouth && hasEast && hasWest) // straight east-west
                || (hasNorth && hasSouth && hasEast && hasWest)); // all sides
    }

    private boolean isConnectable(@NotNull Block block) {
        return BlockTags.WALLS.contains(block.namespace()) || BlockTags.GLASS_PANES.contains(block.namespace()) || block.id() == Block.IRON_BARS.id();
    }
}
