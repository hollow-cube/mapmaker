package net.hollowcube.common.events;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.network.packet.client.play.ClientSelectBundleItemPacket;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public record SelectBundleSlotEvent(
    @NotNull AbstractInventory inventory,
    @NotNull Player player,
    int slot,
    int index
) implements InventoryEvent, PlayerInstanceEvent {

    @Override
    public @NotNull AbstractInventory getInventory() {
        return this.inventory;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    @ApiStatus.Internal
    public static void post(@NotNull ClientSelectBundleItemPacket packet, @NotNull Player player) {
        var openInventory = player.getOpenInventory();
        var slot = packet.slot();
        if (openInventory != null && slot < openInventory.getInnerSize()) {
            EventDispatcher.call(new SelectBundleSlotEvent(openInventory, player, slot, packet.selectedIndex()));
        } else if (openInventory == null) {
            slot = PlayerInventoryUtils.convertWindowSlotToMinestomSlot(slot, 0);
            EventDispatcher.call(new SelectBundleSlotEvent(player.getInventory(), player, slot, packet.selectedIndex()));
        } else {
            slot = PlayerInventoryUtils.convertWindowSlotToMinestomSlot(slot, openInventory.getInnerSize());
            EventDispatcher.call(new SelectBundleSlotEvent(player.getInventory(), player, slot, packet.selectedIndex()));
        }
    }
}
