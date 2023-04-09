package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.BaseElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryCondition;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class InventoryViewHost {
    private static final System.Logger logger = System.getLogger(InventoryViewHost.class.getName());

    private int width, height;
    private InventoryWrapper inventory = null;

    private BaseElement element = null;

    private boolean dirty = false;
    // Start in deferred mode in case the view is marked dirty immediately (eg during constructor)
    private boolean deferredDirty = true;

    // The number of rows of the player inventory currently in use.
    private int playerInventoryRows = 0;

    private final Deque<View> history = new ArrayDeque<>();

    public void pushView(@NotNull View view) {
        history.addLast(view);
        replaceInventory((BaseElement) view.element());
    }

    public void popView() {
        if (history.size() == 0) {
            inventory.getViewers().forEach(Player::closeInventory);
            return;
        }

        history.removeLast();
        replaceInventory((BaseElement) history.getLast().element());

    }

    public @NotNull Inventory getHandle() {
        return inventory;
    }

    private void replaceInventory(@NotNull BaseElement newElement) {
        // Unmount old component if relevant
        Inventory oldInv = this.inventory;
        if (this.element != null) {
            this.element.performSignal(Element.SIG_UNMOUNT);
            // Remove inventory condition immediately, do not want ghost clicks or anything
            this.inventory.getInventoryConditions().clear();
        }

        var name = this.inventory == null ? Component.text("Chest") : this.inventory.getTitle();
        this.inventory = new InventoryWrapper(updateSize(newElement), name);
        this.element = newElement;

        // Mount the contents in this inventory
        this.element.performSignal(Element.SIG_MOUNT);
        deferredDirty = false;
        drawCurrentElement();

        if (oldInv != null) {
            // Migrate the viewers to the new inventory
            for (Player player : oldInv.getViewers()) {
                player.openInventory(inventory);
            }
        }
    }

    private @NotNull InventoryType updateSize(@NotNull BaseElement element) {
//        Check.argCondition(section.width() > 9, "section width must be <= 9, was {}", section.width());
        //todo support for non-9 width inventories
        Check.argCondition(element.width() != 9, "section width must be 9");
        Check.argCondition(element.height() > 10, "section height must be <= 10, was {}", element.height());
        width = element.width();
        height = element.height();

        // Special case for anvil GUIs
//        if (element instanceof AnvilSection) {
//            playerInventoryRows = section.height() - 1;
//            return InventoryType.ANVIL;
//        }

        return switch (element.height()) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            case 6 -> InventoryType.CHEST_6_ROW;
            case 7, 8, 9, 10 -> {
                playerInventoryRows = element.height() - 6;
                yield InventoryType.CHEST_6_ROW;
            }
            default -> throw new IllegalStateException("Unreachabe");
        };
    }

    public void performSignal(@NotNull String signal, @NotNull Object... args) {
        try {
            element.performSignal(signal, args);
        } catch (Exception e) {
            throw new RuntimeException("failed to perform signal", e);
        }
    }

    // Rendering

    public void markDirty() {
        dirty = true;

        // If not deferred, redraw immediately.
        if (!deferredDirty) {
            drawCurrentElement();
        }
    }

    private void drawCurrentElement() {
        // This depends on the fact that we only accept 9-width inventories currently

        var contents = element.getContents();
        ItemStack[] top, bottom = null;

        top = new ItemStack[9 * (height - playerInventoryRows)];
        Arrays.fill(top, ItemStack.AIR);
        for (int i = 0; i < top.length; i++) {
            if (contents[i] == null) continue;
            top[i] = contents[i];
        }

        if (playerInventoryRows > 0) {
            bottom = new ItemStack[9 * playerInventoryRows];
            Arrays.fill(bottom, ItemStack.AIR);
            for (int i = 0; i < bottom.length; i++) {
                if (contents[9 * (height - playerInventoryRows) + i] == null) continue;
                bottom[i] = contents[9 * (height - playerInventoryRows) + i];
            }
        }

        // Build the title string
        var titleBuilder = new StringBuilder();
        element.buildTitle(titleBuilder);

        inventory.replaceInventories(top, bottom);
        inventory.setTitle(Component.text(titleBuilder.toString(), NamedTextColor.WHITE));
        dirty = false;
    }

    private class InventoryWrapper extends Inventory {

        private final InventoryCondition playerCondition = this::playerInvClick;
        private final ItemStack[] playerInv = new ItemStack[9 * 4];

        public InventoryWrapper(@NotNull InventoryType inventoryType, @NotNull Component title) {
            super(inventoryType, title);
            Arrays.fill(playerInv, ItemStack.AIR);
            update();

            addInventoryCondition(this::openedInvClick);
        }

        public boolean needsPlayerInventory() {
            return playerInventoryRows > 0;
        }

        @Override
        public boolean addViewer(@NotNull Player player) {
            var result = super.addViewer(player);
            if (result && ensureDelegatingPlayerInventory(player)) {
                player.getInventory().addInventoryCondition(playerCondition);
                updatePlayerInventory();
            }
            return result;
        }

        @Override
        public boolean removeViewer(@NotNull Player player) {
            var result = super.removeViewer(player);
            if (result) {
                player.getInventory().getInventoryConditions().remove(playerCondition);
                player.getInventory().update();
            }
            return result;
        }

        public void replaceInventories(@NotNull ItemStack @NotNull [] top, @NotNull ItemStack @Nullable [] bottom) {
            // Replace the top inventory
            Arrays.fill(itemStacks, ItemStack.AIR);
            System.arraycopy(top, 0, itemStacks, 0, Math.min(top.length, itemStacks.length));

            // Replace the player inventory if present
            if (bottom != null) {
                Arrays.fill(playerInv, ItemStack.AIR);
                System.arraycopy(bottom, 0, playerInv, 0, Math.min(bottom.length, playerInv.length));
            }

            update();
            updatePlayerInventory();
        }

        public void updatePlayerInventory() {
            getViewers().forEach(player -> player.sendPacket(createWindowItemsPacket(player)));
        }

        private void openedInvClick(@NotNull Player player, int slot, @NotNull ClickType clickType, @NotNull InventoryConditionResult result) {
            var allow = tryHandleClick(slot, player, clickType);
            result.setCancel(!allow);
        }

        private void playerInvClick(@NotNull Player player, int slot, @NotNull ClickType clickType, @NotNull InventoryConditionResult result) {
            slot = convertPlayerSlotToChestSlot(slot);
            if (slot == -1) return; // Not a slot we care about (armor, crafting, off hand)
            //todo hardcoded double chest
            slot = 9 * 6 + slot; // Offset to the bottom of the chest

            var allow = tryHandleClick(slot, player, clickType);
            result.setCancel(!allow);
        }

        private boolean tryHandleClick(int index, @NotNull Player player, @NotNull ClickType clickType) {
            if (element == null) return false;

            deferredDirty = true;
            var result = element.handleClick(player, index, clickType);
            deferredDirty = false;
            if (dirty) drawCurrentElement();
            return result;
        }

        private WindowItemsPacket createWindowItemsPacket(@NotNull Player player) {
            ItemStack[] convertedSlots = new ItemStack[PlayerInventory.INVENTORY_SIZE];
            Arrays.fill(convertedSlots, ItemStack.AIR);
            for (int i = 0; i < convertedSlots.length; i++) {
                var packetSlot = PlayerInventoryUtils.convertToPacketSlot(i);
                var chestSlot = convertPlayerSlotToChestSlot(i);
                if (i < 9 * 4 && chestSlot < 9 * playerInventoryRows) {
                    convertedSlots[packetSlot] = playerInv[chestSlot];
                } else {
                    convertedSlots[packetSlot] = player.getInventory().getItemStack(i);
                }
            }
            return new WindowItemsPacket((byte) 0, 0, List.of(convertedSlots), getCursorItem(player));
        }

        private int convertPlayerSlotToChestSlot(int slot) {
            if (0 <= slot && slot < 9) {
                // Hotbar
                return slot + (9 * 3);
            } else if (9 <= slot && slot < 36) {
                // Main inventory
                return slot - 9;
            } else return -1;
        }

        private boolean ensureDelegatingPlayerInventory(@NotNull Player player) {
            if (player.getInventory() instanceof DelegatingPlayerInventory)
                return true;

            // Update the inventory with reflection
            try {
                var field = Player.class.getDeclaredField("inventory");
                field.setAccessible(true); // NOSONAR
                field.set(player, new DelegatingPlayerInventory(player)); // NOSONAR
                return true;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.log(System.Logger.Level.ERROR, "failed to update player inventory to delegating inventory. this is required for extended inventory support. ", e);
                player.closeInventory();
                return false;
            }
        }

    }

    private static class DelegatingPlayerInventory extends PlayerInventory {

        public DelegatingPlayerInventory(@NotNull Player player) {
            super(player);

            // Copy all contents of players current inventory to this one
            var old = player.getInventory();
            // PlayerInventory
            setCursorItem(old.getCursorItem());
            // AbstractInventory
            System.arraycopy(old.getItemStacks(), 0, itemStacks, 0, INVENTORY_SIZE);
            getInventoryConditions().addAll(old.getInventoryConditions());
            tagHandler().updateContent(old.tagHandler().asCompound());
        }

        @Override
        public void update() {
            // In case we are in an InventoryWrapper (a canvas managed inventory), and it needs player inventory updates,
            // forward the update there and do not call the super method.
            if (player.getOpenInventory() instanceof InventoryWrapper wrapper && wrapper.needsPlayerInventory()) {
                wrapper.updatePlayerInventory();
                return;
            }

            super.update();
        }
    }
}
