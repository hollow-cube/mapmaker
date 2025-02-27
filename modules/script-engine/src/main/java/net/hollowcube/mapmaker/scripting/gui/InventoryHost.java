package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.gui.node.Node;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class InventoryHost {
    Value reconcilerRoot; // Cache for gui manager, should not be modified here.
    private final GuiManager owner;
    private final Player player;

    private final InventoryWrapper handle;


    public Node root; // TODO: do we need multiple roots?

    public InventoryHost(@NotNull GuiManager owner, @NotNull Player player) {
        this.owner = owner;
        this.player = player;
    }

    public @NotNull Inventory handle() {
        return handle;
    }

    public void addChild(@NotNull Node node) {
        if (this.root != null) {
            throw new IllegalStateException("The root of a GUI must be a single element");
        }

        this.root = node;
    }

    public Component titleTemp;
    public ItemStack[] itemsTemp;

    public ItemStack[] build() {
        MenuBuilder builder = new MenuBuilder(9, 10);
        this.root.build(builder);
        itemsTemp = builder.getItems();
        titleTemp = builder.getTitle();
        return itemsTemp;
    }

    private static final class InventoryWrapper extends Inventory {

        // Represents the player inventory slots which will be sent if #needsPlayerInventory is true
        private final ItemStack[] playerInventory = new ItemStack[9 * 4];

        public InventoryWrapper(@NotNull InventoryType inventoryType, @NotNull Component title) {
            super(inventoryType, title);
            Arrays.fill(playerInv, ItemStack.AIR);
            update();
        }

        public boolean needsPlayerInventory() {
            return true; // Always consume player inventory for now.
        }
    }

    /**
     * Exists to delegate "#update" calls when a gui is open and consuming the player inventory.
     */
    private static final class DelegatingPlayerInventory extends PlayerInventory {
        private final Player player;

        public DelegatingPlayerInventory(@NotNull Player player) {
            this.player = player;

            // Copy all contents of players current inventory to this one
            var old = player.getInventory();
            viewers.addAll(old.getViewers());
            // PlayerInventory
            setCursorItem(old.getCursorItem());
            // AbstractInventory
            System.arraycopy(old.getItemStacks(), 0, itemStacks, 0, getSize());
            tagHandler().updateContent(old.tagHandler().asCompound());
        }
    }
}
