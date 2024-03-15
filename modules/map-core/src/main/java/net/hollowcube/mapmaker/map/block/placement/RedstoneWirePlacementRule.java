package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class RedstoneWirePlacementRule extends BaseBlockPlacementRule {
    public static final String NONE = "none";
    public static final String SIDE = "side";
    public static final String UP = "up";

    public RedstoneWirePlacementRule(@NotNull Block block) {
        super(block);

        var lowPriorityChild = EventNode.event("redstone-wire-placement-rule", EventFilter.ALL, e -> e instanceof PlayerBlockBreakEvent);
        lowPriorityChild.setPriority(Integer.MIN_VALUE);
        lowPriorityChild.addListener(PlayerBlockBreakEvent.class, this::handleGlobalBlockBreak);
        MinecraftServer.getGlobalEventHandler().addChild(lowPriorityChild);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placement) {
        var instance = placement.instance();
        var blockPosition = placement.placePosition();

        var newBlock = computeShape(instance, blockPosition);
        hackImmediateNeighborUpdates(instance, blockPosition, newBlock);
        return newBlock;
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState update) {
        // No updates if updated from top or bottom (we do not remove redstone without a block below)
        if (update.fromFace() == BlockFace.TOP || update.fromFace() == BlockFace.BOTTOM)
            return update.currentBlock();

        var instance = update.instance();
        var blockPosition = update.blockPosition();

        return computeShape(instance, blockPosition);
    }

    @Override
    public int maxUpdateDistance() {
        return 2; // Need to go around over + down a corner
    }

    private @NotNull Block computeShape(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
        var north = computeFaceState(instance, blockPosition, BlockFace.NORTH);
        var east = computeFaceState(instance, blockPosition, BlockFace.EAST);
        var south = computeFaceState(instance, blockPosition, BlockFace.SOUTH);
        var west = computeFaceState(instance, blockPosition, BlockFace.WEST);
        int conns = (NONE.equals(north) ? 0 : 1) + (NONE.equals(south) ? 0 : 1) + (NONE.equals(east) ? 0 : 1) + (NONE.equals(west) ? 0 : 1);

        if (conns == 0) {
            north = east = south = west = SIDE;
        } else if (conns == 1) {
            if (!NONE.equals(north)) south = SIDE;
            else if (!NONE.equals(south)) north = SIDE;
            else if (!NONE.equals(east)) west = SIDE;
            else east = SIDE;
        }

        return block.withProperties(Map.of(
                "north", north, "east", east,
                "south", south, "west", west
        ));
    }

    private @NotNull String computeFaceState(@NotNull Block.Getter instance, @NotNull Point originPosition, @NotNull BlockFace face) {
        var relative = originPosition.relative(face);

        // We favor the top here, but only if the above block is non-conductive
        if (isSignalSource(instance, relative.add(0, 1, 0), face) && !isConductive(instance, originPosition.add(0, 1, 0)))
            return UP;

        // Otherwise check either side
        if (isSignalSource(instance, relative, face) || isSignalSource(instance, relative.add(0, -1, 0), face))
            return SIDE;

        return NONE;
    }

    public static boolean isSignalSource(@NotNull Block.Getter instance, @NotNull Point blockPosition, @NotNull BlockFace face) {
        var block = instance.getBlock(blockPosition, Block.Getter.Condition.TYPE);
        if (!block.registry().isSignalSource()) return false;

        if (block.id() == Block.OBSERVER.id() && facing(block) != face)
            return false;
        if (block.id() == Block.REPEATER.id() && facing(block) != face && facing(block) != face.getOppositeFace())
            return false;

        return true;
    }

    public static boolean isConductive(@NotNull Block.Getter instance, @NotNull Point blockPosition) {
        return instance.getBlock(blockPosition, Block.Getter.Condition.TYPE).registry().isRedstoneConductor();
    }

    private static @NotNull BlockFace facing(@NotNull Block block) {
        return BlockFace.valueOf(block.getProperty("facing").toUpperCase(Locale.ROOT));
    }

    // Below is the diagonal update propagation logic.
    // Minestom placement rules do not propagate through blocks without placement rules, meaning we cannot
    // correctly update a redstone wire going up or down a block.
    // Instead, we go to the instance and trigger updates ourselves on the diagonal redstone blocks.
    // This is a bad solution until Minestom gets a better one.

    private void handleGlobalBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        if (event.isCancelled() || event.getBlock().id() != Block.REDSTONE_WIRE.id()) return;
        var rule = MinecraftServer.getBlockManager().getBlockPlacementRule(event.getBlock());
        if (!(rule instanceof RedstoneWirePlacementRule)) return;

        hackImmediateNeighborUpdates(event.getInstance(), event.getBlockPosition(), event.getResultBlock());
    }

    // This is required because in the place rule the block has not been set in the instance, meaning a call to triggerNeighborUpdates
    // will be invalid because its still prior block (eg air). So we set the block in instance immediately, and then later
    // the placement rule will overwrite it with the correct block.
    private void hackImmediateNeighborUpdates(@NotNull Block.Getter blockGetter, @NotNull Point blockPosition, @NotNull Block newBlock) {
        if (!(blockGetter instanceof Instance instance)) return;

        instance.setBlock(blockPosition, newBlock); // Set block immediately, although placement logic will overwrite later.
        triggerNeighborUpdates(blockGetter, blockPosition); // Now valid to run neighbor updates.
    }

    /**
     * Triggers a blockUpdate on every redstone wire +1 or -1 y on each horizontal direction.
     */
    private void triggerNeighborUpdates(@NotNull Block.Getter blockGetter, @NotNull Point blockPosition) {
        if (!(blockGetter instanceof Instance instance)) return;

        for (var face : HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy += 2) {
                var neighbor = blockPosition.add(0, dy, 0).relative(face);
                var block = instance.getBlock(neighbor);
                if (block.id() != Block.REDSTONE_WIRE.id()) continue;

                var newBlock = blockUpdate(new UpdateState(instance, neighbor, block, face.getOppositeFace()));
                instance.setBlock(neighbor, newBlock);
            }
        }
    }
}
