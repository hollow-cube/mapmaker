package net.hollowcube.mapmaker.panels;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InventoryHostEvents {

    @Nullable
    public static <T extends InventoryEvent & PlayerInstanceEvent> ObjectIntPair<InventoryHost> getHostAndSlot(@NotNull T event, int slot) {
        final InventoryHost host;
        if (event.getInventory() instanceof InventoryHost.InventoryWrapper inventory) {
            host = inventory.owner(); // Click in an inventory
        } else if (event.getInventory() instanceof PlayerInventory && event.getPlayer().getOpenInventory() instanceof InventoryHost.InventoryWrapper inventory) {
            host = inventory.owner(); // Click in player inventory

            // Slot needs to be offset from the top of the main inventory
            int offsetSlot = slot;
            int mainSize = getInterpretedSize(host.handle.getInventoryType());

            // We need to reorder the hotbar to come last
            slot = mainSize + offsetSlot + (offsetSlot < 9 ? 27 : -9);
        } else {
            return null;
        }

        // In case we are already handling a click, we should not accept another one.
        if (host.pendingClick != null && !host.pendingClick.isDone()) return null;
        if (host.viewStack.isEmpty()) return null;

        return ObjectIntPair.of(host, slot);
    }

    /**
     * We have special handling for inventories that don't exactly match the expected grid/column layout we use
     * for inventories. For example an anvil has its 3 special slots but we pretend it has 9 slots for the sake
     * of the layout system. Slots 3-8 are skipped in that case.
     *
     * @return The size of the inventory as interpreted by the layout system.
     */
    private static int getInterpretedSize(@NotNull InventoryType type) {
        return switch (type) {
            case CHEST_1_ROW, CHEST_2_ROW, CHEST_3_ROW,
                 CHEST_4_ROW, CHEST_5_ROW, CHEST_6_ROW -> type.getSize();
            case ANVIL, CARTOGRAPHY -> 9;
            default -> throw new IllegalStateException("Unsupported inventory type: " + type);
        };
    }
}
