package net.hollowcube.map.block.rule;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WallPlacementRule extends BlockPlacementRule {
    //todo it should not connect to fence gates

    public WallPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var instance = updateState.instance();
        var blockPosition = updateState.blockPosition();
        var pos = updateState.blockPosition();
        int x = blockPosition.blockX();
        int y = blockPosition.blockY();
        int z = blockPosition.blockZ();

        String north = "none", south = "none";
        String east = "none", west = "none";
        var above = instance.getBlock(x, y + 1, z, Block.Getter.Condition.TYPE);
        if (isSolid(instance, pos, BlockFace.EAST))
            east = above.isSolid() && hasFacingConnection(above, BlockFace.EAST) ? "tall" : "low";
        if (isSolid(instance, pos, BlockFace.WEST))
            west = above.isSolid() && hasFacingConnection(above, BlockFace.WEST) ? "tall" : "low";
        if (isSolid(instance, pos, BlockFace.SOUTH))
            south = above.isSolid() && hasFacingConnection(above, BlockFace.SOUTH) ? "tall" : "low";
        if (isSolid(instance, pos, BlockFace.NORTH))
            north = above.isSolid() && hasFacingConnection(above, BlockFace.NORTH) ? "tall" : "low";

        var aboveIsUp = "true".equals(above.getProperty("up"));
        var up = String.valueOf(!(!aboveIsUp && (
                (!north.equals("none") && !south.equals("none") && east.equals("none") && west.equals("none")) ||
                (north.equals("none") && south.equals("none") && !east.equals("none") && !west.equals("none"))
        )));

        return block.withProperties(Map.of(
                "east", east,
                "north", north,
                "south", south,
                "west", west,
                "up", up
        ));
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        return block;
    }

    private boolean isSolid(Block.Getter instance, @NotNull Point pos, @NotNull BlockFace dir) {
        var block = instance.getBlock(pos.relative(dir), Block.Getter.Condition.TYPE);
        return block.isSolid();
    }

    private boolean hasFacingConnection(@NotNull Block block, @NotNull BlockFace dir) {
        // This pretends that anything without a side property is a connection
        var value = block.getProperty(dir.name().toLowerCase());
        return !("false".equals(value) || "none".equals(value));
    }
}
