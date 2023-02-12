package net.hollowcube.canvas.section;

import net.hollowcube.canvas.section.std.AnvilSection;
import net.kyori.adventure.text.Component;
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
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public sealed class RootSection extends ParentSection permits RouterSection {
    private int width, height;
    private InventoryWrapper inventory;
    private Section section = null;

    // The number of rows of the player inventory currently in use.
    private int playerInventoryRows = 0;

    private final Map<Class<?>, Object> context;

    public RootSection(@NotNull Section section) {
        this(section, Map.of());
    }

    public RootSection(@NotNull Section section, @NotNull Map<Class<?>, Object> context) {
        // We override these methods, so their value is just a marker in case it crops up somewhere it shouldnt.
        super(Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.context = context;

        replaceInventory(section);
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public void showToPlayer(@NotNull Player player) {
        player.openInventory(getInventory());
    }

    @Override
    public <C extends Section> @UnknownNullability C find(Class<C> componentType) {
        if (componentType.isAssignableFrom(getClass())) return componentType.cast(this);
        return null;
    }

    public void setTitle(@NotNull net.kyori.adventure.text.Component title) {
        inventory.setTitle(title);
    }

    // Item manipulation

    @Override
    void updateItem(int index, @NotNull ItemStack itemStack) {
        Check.argCondition(index < 0 || index >= width * height, "index out of bounds");

        // If it is inside the open inventory, set it directly.
        //todo hardcoded to chest inventory, will need to be changed for other inventory types
        if (index < 9 * 6) {
            //todo does not need to call item events, and should wait/batch updates on tick perhaps
            inventory.setItemStack(index, itemStack);
            return;
        }

        // Otherwise, set it in the (ghost) player inventory.
        //todo hardcoded double chest size
        inventory.setPlayerItemStack(index - 9 * 6, itemStack);
    }

    // Click handling

    private boolean tryHandleClick(int index, @NotNull Player player, @NotNull ClickType clickType) {
        if (index == -999) {
            // Clicked outside inventory, todo handle this properly
            return false;
        }

        // Ensure the index is inside the component
        int x = index % width();
        int y = index / width();
        if (x >= section.width() || y >= section.height()) {
            return false;
        }

        // Convert the index to the component's coordinate system and call the component's click handler
        index = x + y * section.width();
        return section.handleClick(index, player, clickType);
    }

    // Internal details


    @Override
    int _width() {
        return width;
    }

    @Override
    int _height() {
        return height;
    }

    protected void replaceInventory(@NotNull Section newSection) {
        // Unmount old component if relevant
        Inventory oldInv = this.inventory;
        if (this.section != null) {
            // Remove inventory condition immediately, do not want ghost clicks or anything
            this.inventory.getInventoryConditions().clear();
            this.section.removeParent();
        }

        var name = this.inventory == null ? Component.text("Chest") : this.inventory.getTitle();
        this.inventory = new InventoryWrapper(updateSize(newSection), name);
        this.section = newSection;
        this.section.setParent(this, 0);

        if (oldInv != null) {
            // Migrate the viewers to the new inventory
            for (Player player : oldInv.getViewers()) {
                player.openInventory(inventory);
            }
        }
    }

    private @NotNull InventoryType updateSize(@NotNull Section section) {
//        Check.argCondition(section.width() > 9, "section width must be <= 9, was {}", section.width());
        //todo support for non-9 width inventories
        Check.argCondition(section.width() != 9, "section width must be 9");
        Check.argCondition(section.height() > 10, "section height must be <= 10, was {}", section.height());
        width = section.width();
        height = section.height();

        // Special case for anvil GUIs
        if (section instanceof AnvilSection) {
            playerInventoryRows = section.height() - 1;
            return InventoryType.ANVIL;
        }

        return switch (section.height()) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            case 6 -> InventoryType.CHEST_6_ROW;
            case 7, 8, 9, 10 -> {
                playerInventoryRows = section.height() - 6;
                yield InventoryType.CHEST_6_ROW;
            }
            default -> throw new IllegalStateException("Unreachabe");
        };
    }

    @Override
    protected <T> @NotNull T getContext(@NotNull Class<T> type) {
        return type.cast(context.get(type));
    }

    private class InventoryWrapper extends Inventory {
        private static final Logger logger = LoggerFactory.getLogger(InventoryWrapper.class);

        private final InventoryCondition playerCondition = this::playerInvClick;

        private final ItemStack[] playerInv = new ItemStack[9 * 4];

        public InventoryWrapper(@NotNull InventoryType inventoryType, @NotNull Component title) {
            super(inventoryType, title);
            Arrays.fill(playerInv, ItemStack.AIR);
            update();

            addInventoryCondition(this::openedInvClick);
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

        public boolean needsPlayerInventory() {
            return playerInventoryRows > 0;
        }

        // Takes slot as a player inventory slot (eg 0-35 from top to bottom)
        public void setPlayerItemStack(int slot, @NotNull ItemStack itemStack) {
            playerInv[slot] = itemStack;
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

        /** Ensures that the current PlayerInventory is a {@link DelegatingPlayerInventory}. */
        private boolean ensureDelegatingPlayerInventory(@NotNull Player player) {
            if (player.getInventory() instanceof DelegatingPlayerInventory) return true;

            // Update the field with reflection
            try {
                var field = Player.class.getDeclaredField("inventory");
                field.setAccessible(true);
                field.set(player, new DelegatingPlayerInventory(player));
                return true;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("failed to update player inventory to delegating inventory. this is required for extended inventory support. ", e);
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
