package net.hollowcube.common.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.listener.CreativeInventoryActionListener;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import org.jetbrains.annotations.ApiStatus;

public final class PlayerGiveCreativeItemEvent implements PlayerInstanceEvent, CancellableEvent {

    private final Player player;
    private final ItemStack item;
    private final short slot;

    private boolean cancelled;

    public PlayerGiveCreativeItemEvent(Player player, ItemStack item, short slot) {
        this.player = player;
        this.item = item;
        this.slot = slot;
    }

    @Override
    public Player getPlayer() {
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

    public ItemStack item() {
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
