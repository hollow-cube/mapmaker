package net.hollowcube.common.events;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.listener.PlayerActionListener;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import org.jetbrains.annotations.ApiStatus;

public final class PlayerHitBlockEvent implements PlayerInstanceEvent, BlockEvent, CancellableEvent {

    private final Player player;
    private final Block block;
    private final BlockVec vec;
    private final BlockFace face;

    private boolean cancelled = false;

    public PlayerHitBlockEvent(Player player, Block block, BlockVec vec, BlockFace face) {
        this.player = player;
        this.block = block;
        this.vec = vec;
        this.face = face;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockVec getBlockPosition() {
        return vec;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public BlockFace getFace() {
        return face;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @ApiStatus.Internal
    public static void post(ClientPlayerActionPacket packet, Player player) {
        var block = player.getInstance().getBlock(packet.blockPosition());
        var event = new PlayerHitBlockEvent(player, block, new BlockVec(packet.blockPosition()), packet.blockFace());
        EventDispatcher.call(event);

        if (event.isCancelled()) return;

        PlayerActionListener.playerActionListener(packet, player);
    }
}
