package net.hollowcube.mapmaker.map.feature.play;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.BlockPredicates;
import net.minestom.server.item.component.ItemBlockState;
import net.minestom.server.listener.BlockPlacementListener;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

public class BlockPlacementFeatureProvider {
    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();

    public static void handleBlockPlacementPacket(@NotNull ClientPlayerBlockPlacementPacket packet, @NotNull Player player) {
        if (handleClientsidePlacement(packet, player)) return;

        // Was not handled by us, pass it along to Minestom
        BlockPlacementListener.listener(packet, player);
    }

    // Returns true if handled, false to delegate to Minestom
    private static boolean handleClientsidePlacement(@NotNull ClientPlayerBlockPlacementPacket packet, @NotNull Player player) {
        if (player.getGameMode() != GameMode.ADVENTURE) return false;

        // Basically we want to fast exit if this definitely doesn't look like a block placement we care about.
        final MapWorld world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return false;

        final PlayerHand hand = packet.hand();
        final ItemStack usedItem = player.getItemInHand(hand);
        final Material useMaterial = usedItem.material();
        if (!useMaterial.isBlock()) return false;

        // If placing in an unloaded chunk just ignore the packet entirely.
        final Instance instance = world.instance();
        final Point blockPosition = packet.blockPosition();
        final Chunk interactedChunk = instance.getChunkAt(blockPosition);
        if (!ChunkUtils.isLoaded(interactedChunk)) return true;

        // TODO: Decide at this point if this is a block placement we care about (eg if the item in hand is a placeable block)
        //  One minor note is that we need to start the timer if you place a block

        final var ghostBlockHolder = GhostBlockHolder.forPlayer(player);
        final Block interactedBlock = ghostBlockHolder.getBlock(blockPosition);

        final BlockFace blockFace = packet.blockFace();
        final Point cursorPosition = new Vec(packet.cursorPositionX(), packet.cursorPositionY(), packet.cursorPositionZ());

        // Verify if the player can place the block
        BlockPredicates placePredicate = usedItem.get(DataComponents.CAN_PLACE_ON, BlockPredicates.NEVER);
        final boolean canPlaceBlock = placePredicate.test(interactedBlock);
        if (!canPlaceBlock) {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
            return true;
        }

        // Get the newly placed block position
        Point placementPosition = blockPosition;
        var interactedPlacementRule = BLOCK_MANAGER.getBlockPlacementRule(interactedBlock);
        if (!interactedBlock.isAir() && (interactedPlacementRule == null || !interactedPlacementRule.isSelfReplaceable(
                new BlockPlacementRule.Replacement(interactedBlock, blockFace, cursorPosition, false, useMaterial)))) {
            // If the block is not replaceable, try to place next to it.
            placementPosition = blockPosition.relative(blockFace);

            var placementBlock = ghostBlockHolder.getBlock(placementPosition);
            var placementRule = BLOCK_MANAGER.getBlockPlacementRule(placementBlock);
            if (!placementBlock.registry().isReplaceable() && !(placementRule != null && placementRule.isSelfReplaceable(
                    new BlockPlacementRule.Replacement(placementBlock, blockFace, cursorPosition, true, useMaterial)))) {
                // If the block is still not replaceable, cancel the placement
                player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
                return true;
            }
        }

        // Ignore the placement if trying to place outside the world.
        final DimensionType instanceDim = instance.getCachedDimensionType();
        if (placementPosition.y() >= instanceDim.maxY() || placementPosition.y() < instanceDim.minY() || !instance.getWorldBorder().inBounds(placementPosition)) {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
            return true;
        }

        // Kind of a weird case, just let Minestom handle it like normal.
        final Chunk chunk = instance.getChunkAt(placementPosition);
        if (!ChunkUtils.isLoaded(chunk) || chunk.isReadOnly()) {
            return false;
        }

        final ItemBlockState blockState = usedItem.get(DataComponents.BLOCK_STATE, ItemBlockState.EMPTY);
        final Block placedBlock = blockState.apply(useMaterial.block());

        Entity collisionEntity = CollisionUtils.canPlaceBlockAt(instance, placementPosition, placedBlock);
        if (collisionEntity != null) {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
            return true;
        }

        // Update the block on the client and record it for the checkpoint.
        ghostBlockHolder.setBlock(placementPosition, placedBlock);
        player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        final ItemStack newUsedItem = usedItem.consume(1);
        player.setItemInHand(hand, newUsedItem);

        return true; // Done :)
    }
}
