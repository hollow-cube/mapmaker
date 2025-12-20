package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.kyori.adventure.key.Key;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.HashSet;
import java.util.Set;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

/**
 * For blocks in which the interactions should delayed ie. trapdoors resetting in play mode.
 */
public class DelayedBlockInteractions {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(PlayerBlockInteractEvent.class, DelayedBlockInteractions::handleBlockInteraction);

    private static final Set<Key> DELAYED_BLOCKS = new HashSet<>();

    static {
        DELAYED_BLOCKS.addAll(BlockTags.TRAPDOORS);
        DELAYED_BLOCKS.addAll(BlockTags.DOORS);
        DELAYED_BLOCKS.addAll(BlockTags.FENCE_GATES);
    }

    private static void handleBlockInteraction(PlayerBlockInteractEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        if (world == null || player.isSneaking()) return;
        if (!DELAYED_BLOCKS.contains(event.getBlock().key())) return;
        if (!(player instanceof MapPlayer mp) || mp.canSendPose()) return;

        var pos = event.getBlockPosition();
        var block = event.getBlock();
        var open = Boolean.parseBoolean(block.getProperty("open"));

        player.sendPacket(new BlockChangePacket(pos, block.withProperty("open", String.valueOf(!open))));

        player.scheduler().scheduleEndOfTick(() -> player.sendPacket(new BlockChangePacket(pos, block)));

        event.setCancelled(true);
        event.setBlockingItemUse(true);
    }
}
