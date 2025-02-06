package net.hollowcube.common.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.listener.CreativeInventoryActionListener;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class PlayerGiveCreativeItemEvent implements PlayerInstanceEvent, CancellableEvent {

    private final @NotNull Player player;
    private final @NotNull ItemStack item;
    private final short slot;

    private boolean cancelled;

    public PlayerGiveCreativeItemEvent(
            @NotNull Player player,
            @NotNull ItemStack item,
            short slot
    ) {
        this.player = player;
        this.item = item;
        this.slot = slot;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public @NotNull ItemStack item() {
        return item;
    }

    public short slot() {
        return slot;
    }

    @ApiStatus.Internal
    public static void post(ClientCreativeInventoryActionPacket packet, Player player) {
        var event = new PlayerGiveCreativeItemEvent(player, packet.item(), packet.slot());
        EventDispatcher.call(event);

        if (!event.isCancelled()) {
            CreativeInventoryActionListener.listener(packet, player);
        }
    }


}
