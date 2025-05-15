package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerAnvilInputEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.OpenWindowPacket;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * InventoryHost manages the view stack, 'rendering' of the current view, and user interactions for the
 * single open GUI represented by this InventoryHost.
 */
public class InventoryHost {
    private static final Logger logger = LoggerFactory.getLogger(InventoryHost.class);
    private static final Sound CLICK_SOUND = Sound.sound(SoundEvent.UI_BUTTON_CLICK, Sound.Source.PLAYER, 0.2f, 1f);
    private static final ThreadLocal<InventoryHost> CURRENT = new ThreadLocal<>();

    public static @NotNull InventoryHost current() {
        return Objects.requireNonNull(CURRENT.get(), "No current InventoryHost");
    }

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(InventoryPreClickEvent.class, InventoryHost::handleInventoryClick)
                .addListener(PlayerAnvilInputEvent.class, InventoryHost::handleAnvilInput);
    }

    private final Player player;
    private final InventoryWrapper handle = new InventoryWrapper();

    private Task redrawTask = null;
    // If present, indicates a potentially pending click event. We should not respond to other clicks while this
    // is both not null and not CompletableFuture#isDone.
    private CompletableFuture<Void> pendingClick = null;

    private final List<Panel> panels = new ArrayList<>();
    private final List<InventoryType> inventoryTypes = new ArrayList<>();

    public InventoryHost(@NotNull Player player) {
        this.player = player;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull Inventory handle() {
        return Objects.requireNonNull(handle);
    }

    public void pushView(@NotNull Panel panel) {
        if (!panels.isEmpty()) {
            var last = panels.getLast();
            if (last != null) last.unmount();
        }

        this.inventoryTypes.add(panel.inventoryType());
        this.panels.add(panel);
        panel.mount(this, true);

        drawCurrentElement();
    }

    public void popView() {
        if (panels.size() <= 1) return;

        var removed = this.panels.removeLast();
        if (removed != null) removed.unmount();
        this.inventoryTypes.removeLast();

        panels.getLast().mount(this, false);

        drawCurrentElement();
    }

    public boolean canPopView() {
        return panels.size() > 1;
    }

    public void queueRedraw() {
        if (this.redrawTask != null && this.redrawTask.isAlive()) return;
        this.redrawTask = player.scheduler().scheduleEndOfTick(this::drawCurrentElement);
    }

    // "rendering" implementation

    private void drawCurrentElement() {
        // Check if unmounted or closed
        if (this.panels.isEmpty()) return;
        final Panel root = this.panels.getLast();

        // Currently we always consume the player inventory so add 4 rows.
        final InventoryType type = this.inventoryTypes.getLast();
        int containerSizeInRows = getInterpretedSize(type) / 9;
        var menuBuilder = new MenuBuilder(9, containerSizeInRows + 4, containerSizeInRows);
        root.build(menuBuilder);

        if (!handle.isViewer(player))
            player.openInventory(handle);
        this.handle.updateContents(type, menuBuilder.getItems(), menuBuilder.getTitle());
    }

    private static void handleInventoryClick(@NotNull InventoryPreClickEvent event) {
        final int slot;
        final InventoryHost host;
        if (event.getInventory() instanceof InventoryWrapper inventory) {
            host = inventory.owner(); // Click in an inventory
            slot = event.getClick().slot();
        } else if (event.getInventory() instanceof PlayerInventory &&
                event.getPlayer().getOpenInventory() instanceof InventoryWrapper inventory) {
            host = inventory.owner(); // Click in player inventory

            // Slot needs to be offset from the top of the main inventory
            int offsetSlot = event.getClick().slot();
            int mainSize = getInterpretedSize(host.handle.getInventoryType());

            // We need to reorder the hotbar to come last
            slot = mainSize + offsetSlot + (offsetSlot < 9 ? 27 : -9);
        } else return; // Don't care about this click.

        // In case we are already handling a click, we should not accept another one.
        if (host.pendingClick != null && !host.pendingClick.isDone()) {
            return;
        }

        // At this point we always consume the click
        event.setCancelled(true);
        final ClickType clickType = switch (event.getClick()) {
            case Click.Left ignored -> ClickType.LEFT_CLICK;
            case Click.Right ignored -> ClickType.RIGHT_CLICK;
            case Click.LeftShift ignored -> ClickType.SHIFT_LEFT_CLICK;
            case Click.RightShift ignored -> ClickType.SHIFT_LEFT_CLICK; // todo should be right
            default -> null;
        };
        if (clickType == null) return;

        if (host.panels.isEmpty()) return;
        var root = host.panels.getLast();
        host.pendingClick = root.handleClick(clickType, slot % 9, slot / 9);
        if (host.pendingClick != null) {
            host.player.playSound(CLICK_SOUND);
        }
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

    private static void handleAnvilInput(@NotNull PlayerAnvilInputEvent event) {
        if (!(event.getInventory() instanceof InventoryWrapper inventory)) return;
        var host = inventory.owner();
        if (host.panels.isEmpty()) return;
        if (!(host.panels.getLast() instanceof AbstractAnvilView anvil)) return;

        anvil.handleAnvilInput(event.getInput());
    }

    private final class InventoryWrapper extends Inventory {
        private static final Component UNREACHABLE = Component.text("unreachable");

        private InventoryType inventoryType = InventoryType.CHEST_6_ROW;
        private Component title = Component.empty();

        // Represents the player inventory slots which will be sent if present.
        // May be smaller than the player inventory (eg 9 items) and will show the player items for the rest.
        private ItemStack[] playerInventory = null;

        public InventoryWrapper() {
            // We override handling of inventory type and title. If these values are ever observed, a mistake has been made
            super(InventoryType.CHEST_6_ROW, UNREACHABLE);
        }

        public @NotNull InventoryHost owner() {
            return InventoryHost.this;
        }

        @Override
        public @NotNull InventoryType getInventoryType() {
            return this.inventoryType;
        }

        @Override
        public int getSize() {
            return Objects.requireNonNullElse(this.inventoryType, InventoryType.CHEST_6_ROW).getSize();
        }

        @Override
        public @NotNull Component getTitle() {
            return this.title;
        }

        public void updateContents(@NotNull InventoryType type, @NotNull ItemStack[] items, @NotNull Component title) {
            this.inventoryType = type;
            this.title = title;
            copyInventoryContents(items);

            try {
                sendPacketToViewers(new BundlePacket());
                sendPacketToViewers(new OpenWindowPacket(getWindowId(), getInventoryType().getWindowType(), getTitle()));
                update();
                updatePlayerInventory();
            } finally {
                sendPacketToViewers(new BundlePacket());
            }
        }

        private void copyInventoryContents(@NotNull ItemStack[] items) {
            Check.argCondition(items.length < getSize(), "items length must be at least the size of the inventory");
            System.arraycopy(items, 0, itemStacks, 0, getSize());
            if (items.length > getInterpretedSize(getInventoryType())) {
                this.playerInventory = Arrays.copyOfRange(items, getSize(), items.length);
            } else this.playerInventory = null;
        }

        @Override
        public boolean addViewer(@NotNull Player player) {
            var result = super.addViewer(player);
            if (result && ensureDelegatingPlayerInventory(player)) {
                updatePlayerInventory();
            }
            return result;
        }

        @Override
        public boolean removeViewer(@NotNull Player player) {
            var result = super.removeViewer(player);
            if (result) { // Update the player inventory to show their original items.
                player.getInventory().update();
            }

            if (result && !panels.isEmpty()) {
                panels.getLast().unmount();
                panels.clear();
            }

            return result;
        }

        private int playerInventorySlots() {
            return playerInventory == null ? 0 : playerInventory.length;
        }

        @Override
        public void update(@NotNull Player player) {
            var itemStacks = new ItemStack[getSize()];
            System.arraycopy(this.itemStacks, 0, itemStacks, 0, getSize());
            player.sendPacket(new WindowItemsPacket(getWindowId(), 0, List.of(itemStacks), player.getInventory().getCursorItem()));
        }

        public void updatePlayerInventory() {
            getViewers().forEach(player -> player.sendPacket(createWindowItemsPacket(player)));
        }

        private WindowItemsPacket createWindowItemsPacket(@NotNull Player player) {
            ItemStack[] convertedSlots = new ItemStack[PlayerInventory.INVENTORY_SIZE];
            Arrays.fill(convertedSlots, ItemStack.AIR);
            for (int i = 0; i < convertedSlots.length; i++) {
                var packetSlot = PlayerInventoryUtils.convertMinestomSlotToWindowSlot(i);
                var chestSlot = convertPlayerSlotToChestSlot(i);
                if (i < 9 * 4 && chestSlot < playerInventorySlots()) {
                    convertedSlots[packetSlot] = playerInventory[chestSlot];
                } else {
                    convertedSlots[packetSlot] = player.getInventory().getItemStack(i);
                }
            }
            return new WindowItemsPacket((byte) 0, 0, List.of(convertedSlots), ItemStack.AIR); //todo air was cursor item, does it matter?
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

            try {
                class Holder {
                    static Field inventoryField;
                }
                if (Holder.inventoryField == null) {
                    Holder.inventoryField = Player.class.getDeclaredField("inventory");
                    Holder.inventoryField.setAccessible(true);
                }

                Holder.inventoryField.set(player, new DelegatingPlayerInventory(player));
                return true;
            } catch (Exception e) {
                logger.error("failed to update player inventory to delegating inventory. this is required for extended inventory support. ", e);
                PostHog.captureException(e, player.getUuid().toString());
                return false;
            }
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

        @Override
        public void update() {
            // In case we are in an InventoryWrapper (a canvas managed inventory), and it needs player inventory updates,
            // forward the update there and do not call the super method.
            if (player.getOpenInventory() instanceof InventoryWrapper wrapper && wrapper.playerInventorySlots() > 0) {
                wrapper.updatePlayerInventory();
                return;
            }

            super.update();
        }

        @Override
        public void update(@NotNull Player player) {
            // Same as above (but in case a single player update is called)
            if (player.getOpenInventory() instanceof InventoryWrapper wrapper && wrapper.playerInventorySlots() > 0) {
                wrapper.updatePlayerInventory();
                return;
            }

            super.update(player);
        }
    }

}
