package net.hollowcube.mapmaker.runtime.parkour;

import it.unimi.dsi.fastutil.longs.Long2BooleanArrayMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.tag.Tag;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

/**
 * For blocks in which the interactions should delayed ie. trapdoors resetting in play mode.
 */
public class DelayedBlockInteractions {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(PlayerBlockInteractEvent.class, DelayedBlockInteractions::handleBlockInteraction);

    private static final Tag<Long2BooleanMap> BLOCK_INTERACTIONS_TAG = Tag.Transient("mapmaker:delayed_block_interactions");
    private static final Set<Key> DELAYED_BLOCKS = new HashSet<>();

    static {
        DELAYED_BLOCKS.addAll(BlockTags.TRAPDOORS);
        DELAYED_BLOCKS.addAll(BlockTags.DOORS);
        DELAYED_BLOCKS.addAll(BlockTags.FENCE_GATES);

        DELAYED_BLOCKS.remove(Block.IRON_DOOR.key());
        DELAYED_BLOCKS.remove(Block.IRON_TRAPDOOR.key());
    }

    private static void handleBlockInteraction(PlayerBlockInteractEvent event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        var world = MapWorld.forPlayer(player);
        var holdingItems = !player.getItemInMainHand().isAir() || !player.getItemInOffHand().isAir();
        if (world == null || (player.isSneaking() && holdingItems)) return;
        if (!DELAYED_BLOCKS.contains(block.key())) return;
        if (!(player instanceof MapPlayer mp) || mp.canSendPose()) return;

        var interactions = player.updateAndGetTag(
            BLOCK_INTERACTIONS_TAG,
            it -> it == null ? new Long2BooleanArrayMap() : it
        );
        var pos = event.getBlockPosition();
        var key = (((long) pos.x() & 0x3FFFFFF) << 38) | (((long) pos.z() & 0x3FFFFFF) << 12) | ((long) pos.y() & 0xFFF);
        var open = interactions.getOrDefault(key, Boolean.parseBoolean(block.getProperty("open")));

        var newBlock = BlockTags.FENCE_GATES.contains(block.key()) ?
            handleFenceGate(open, block, player) :
            block.withProperty("open", String.valueOf(!open));

        player.sendPacket(new BlockChangePacket(pos, newBlock));
        interactions.put(key, !open);

        player.scheduler().scheduleEndOfTick(() -> {
            interactions.remove(key);
            player.sendPacket(new BlockChangePacket(pos, block));
        });

        event.setCancelled(true);
        event.setBlockingItemUse(true);
    }

    private static Block handleFenceGate(boolean state, Block block, Player player) {
        var playerDirection = DirectionUtil.fromYaw(player.getPosition());
        var blockDirection = BlockUtil.getFacing(block);
        if (blockDirection == playerDirection.opposite()) {
            block = block.withProperty("facing", playerDirection.name().toLowerCase(Locale.ROOT));
        }
        return block.withProperty("open", String.valueOf(!state));
    }
}
