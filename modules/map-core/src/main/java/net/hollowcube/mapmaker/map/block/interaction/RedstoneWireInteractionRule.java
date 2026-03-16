package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.placement.RedstoneWirePlacementRule;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

import java.util.Locale;
import java.util.Map;

import static net.hollowcube.mapmaker.map.block.placement.RedstoneWirePlacementRule.*;

public class RedstoneWireInteractionRule implements BlockInteractionRule {
    private static final BlockFace[] HORIZONTAL = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };
    private static final String CONNECTED = "";

    public static final RedstoneWireInteractionRule INSTANCE = new RedstoneWireInteractionRule();

    private RedstoneWireInteractionRule() {
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        String newState = null;
        for (var face : HORIZONTAL) {
            // For each state assign it to newState. If
            var faceState = getSideState(interaction, blockPosition, block, face);
            if (newState == null) {
                newState = faceState;
            } else if (newState.equals(faceState)) {
                continue;
            } else return false;
        }

        // Now we are trying to invert, so invert.
        if (SIDE.equals(newState)) newState = NONE;
        else if (NONE.equals(newState)) newState = SIDE;
        else return false;

        interaction.setBlock(blockPosition, block.withProperties(Map.of(
                "north", newState, "south", newState,
                "east", newState, "west", newState
        )));
        return true;
    }

    // Returns NONE if it is NONE, SIDE if it is an unconnected side, and CONNECTED if it is UP or a connected side.
    private String getSideState(Block.Getter instance, Point originPosition, Block block, BlockFace face) {
        var state = block.getProperty(face.name().toLowerCase(Locale.ROOT));
        if (NONE.equals(state)) return NONE;
        if (UP.equals(state)) return CONNECTED;

        // Check if neighbor should be connected on SIDE
        if (RedstoneWirePlacementRule.isSignalSource(instance, originPosition.relative(face), face))
            return CONNECTED;
        return SIDE;
    }

    @Override
    public SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }
}
