package net.hollowcube.common.events;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.listener.PlayerDiggingListener;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class PlayerHitBlockEvent implements PlayerInstanceEvent, BlockEvent, CancellableEvent {

    private final @NotNull Player player;
    private final @NotNull Block block;
    private final @NotNull BlockVec vec;
    private final @NotNull BlockFace face;

    private boolean cancelled = false;

    public PlayerHitBlockEvent(
            @NotNull Player player,
            @NotNull Block block,
            @NotNull BlockVec vec,
            @NotNull BlockFace face
    ) {
        this.player = player;
        this.block = block;
        this.vec = vec;
        this.face = face;
    }

    @Override
    public @NotNull Block getBlock() {
        return block;
    }

    @Override
    public @NotNull BlockVec getBlockPosition() {
        return vec;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull BlockFace getFace() {
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
    public static void post(ClientPlayerDiggingPacket packet, Player player) {
        var block = player.getInstance().getBlock(packet.blockPosition());
        var event = new PlayerHitBlockEvent(player, block, new BlockVec(packet.blockPosition()), packet.blockFace());
        EventDispatcher.call(event);

        if (event.isCancelled()) return;

        PlayerDiggingListener.playerDiggingListener(packet, player);
    }
}
