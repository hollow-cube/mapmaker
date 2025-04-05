package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.gui.node.Node;
import net.hollowcube.mapmaker.scripting.gui.util.ClickType;
import net.hollowcube.posthog.PostHog;
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
import net.minestom.server.network.packet.server.play.OpenWindowPacket;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.validate.Check;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

public class InventoryHost {
    private static final Logger logger = LoggerFactory.getLogger(InventoryHost.class);

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(InventoryPreClickEvent.class, InventoryHost::handleInventoryClick)
                .addListener(PlayerAnvilInputEvent.class, InventoryHost::handleAnvilInput);
    }

    public static final ThreadLocal<InventoryHost> CURRENT = new ThreadLocal<>();

    Value reconcilerRoot; // Cache for gui manager, should not be modified here.
    public final GuiManager owner;
    private final Player player;

    public final Queue<Value> pendingMicrotasks = new ArrayDeque<>();

    private final AtomicReference<Task> redrawTask = new AtomicReference<>();
    private InventoryWrapper handle;

    // One root corresponds to one element. Only the top/last root is rendered.
    public List<Object> elements = new ArrayList<>();
    public List<Node> roots = new ArrayList<>();

    public InventoryHost(@NotNull GuiManager owner, @NotNull Player player) {
        this.owner = owner;
        this.player = player;
    }

    public @NotNull Inventory handle() {
        return Objects.requireNonNull(handle);
    }

    public void pushView(@NotNull Value element) {
        var renderElement = owner.react.exports().invokeMember("createElement",
                owner.react.exports().getMember("Fragment"),
                // TODO: probably should generate a random key rather than using the size of elements.
                proxyObject(Map.of("key", String.valueOf(elements.size()))),
                element);
        elements.add(renderElement);

        owner.render(this, InventoryType.CHEST_3_ROW);
    }

    public void popView() {
        elements.removeLast();
        owner.render(this, InventoryType.CHEST_6_ROW);
    }

    public void jsExit() {
        while (!pendingMicrotasks.isEmpty()) {
            Value microtask = pendingMicrotasks.poll();
            if (microtask == null) break;
            try {
                microtask.executeVoid();
            } catch (Exception e) {
                logger.error("Failed to execute microtask", e);
                PostHog.captureException(e, player.getUuid().toString());
            }
        }
    }

    public void queueRedraw() {
        if (this.handle == null) return;
        final Task oldTask = this.redrawTask.getAndSet(player.scheduler().scheduleEndOfTick(() ->
                this.drawCurrentElement(handle.getInventoryType())));
        if (oldTask != null) oldTask.cancel();
    }

    public void addChild(@NotNull Node node) {
        this.roots.add(node);
        drawCurrentElement(InventoryType.CHEST_6_ROW); // todo
    }

    public void removeChild(@NotNull Node child) {
        var index = this.roots.indexOf(child);
        if (index == -1) throw new IllegalStateException("Child not found: " + child);

        this.roots.remove(index);
    }

    public void clear() {
        this.roots.clear();
    }

    public @Nullable Inventory drawCurrentElement(@NotNull InventoryType type) {
        if (this.roots.isEmpty()) return null; // Check if unmounted
        final Node root = this.roots.getLast();

        // Currently we always consume the player inventory so add 4 rows.
        int containerSizeInRows = getInterpretedSize(type) / 9;
        var menuBuilder = new MenuBuilder(9, containerSizeInRows + 4, containerSizeInRows);
        root.build(menuBuilder);

        if (this.handle != null) {
            this.handle.updateContents(type, menuBuilder.getItems(), menuBuilder.getTitle());
        } else {
            this.handle = new InventoryWrapper(this, type, menuBuilder.getItems(), menuBuilder.getTitle());
            player.openInventory(handle);
        }

        return this.handle;
    }

    /**
     * We have special handling for inventories that don't exactly match the expected grid/column layout we use
     * for inventories. For example an anvil has its 3 special slots but we pretend it has 9 slots for the sake
     * of the layout system. Slots 3-8 are skipped in that case.
     *
     * @return The size of the inventory as interpreted by the layout system.
     */
    public static int getInterpretedSize(@NotNull InventoryType type) {
        return switch (type) {
            case CHEST_1_ROW, CHEST_2_ROW, CHEST_3_ROW,
                 CHEST_4_ROW, CHEST_5_ROW, CHEST_6_ROW -> type.getSize();
            case ANVIL, CARTOGRAPHY -> 9;
            default -> throw new IllegalStateException("Unsupported inventory type: " + type);
        };
    }

    private CompletableFuture<Void> pendingClick = null;

    private static void handleInventoryClick(@NotNull InventoryPreClickEvent event) {
        final int slot;
        final InventoryHost host;
        if (event.getInventory() instanceof InventoryWrapper inventory) {
            host = inventory.owner; // Click in an inventory
            slot = event.getClick().slot();
        } else if (event.getInventory() instanceof PlayerInventory &&
                event.getPlayer().getOpenInventory() instanceof InventoryWrapper inventory) {
            host = inventory.owner; // Click in player inventory

            // Slot needs to be offset from the top of the main inventory
            int offsetSlot = event.getClick().slot();
            int mainSize = getInterpretedSize(host.handle.getInventoryType());

            // We need to reorder the hotbar to come last
            slot = mainSize + offsetSlot + (offsetSlot < 9 ? 27 : -9);
        } else return; // Don't care about this click.

        if (host.roots.isEmpty()) return; // Check for unmounted.
        final Node root = host.roots.getLast();

        if (host.pendingClick != null && !host.pendingClick.isDone()) {
            System.out.println("Click is pending, ignoring click " + host.pendingClick);
            return;
        }

        final ClickType clickType = switch (event.getClick()) {
            case Click.Left ignored -> ClickType.LEFT;
            case Click.Right ignored -> ClickType.RIGHT;
            case Click.LeftShift ignored -> ClickType.LEFT_SHIFT;
            case Click.RightShift ignored -> ClickType.RIGHT_SHIFT;
            default -> null;
        };
        if (clickType == null) {
            event.setCancelled(true);
            return;
        }

        // TODO: need to reintroduce the click locking mechanism.
        //  also probably handleClick should return a future that can complete later.

        try {
            CURRENT.set(host);
            event.setCancelled(true);
            host.pendingClick = root.handleClick(clickType, slot % 9, slot / 9);
            host.jsExit();
        } catch (Exception e) {
            logger.error("Failed to handle click", e);
            PostHog.captureException(e, host.player.getUuid().toString());
        } finally {
            CURRENT.remove();
        }
    }

    private static void handleAnvilInput(@NotNull PlayerAnvilInputEvent event) {
        // TODO
    }

    private static final class InventoryWrapper extends Inventory {
        private final InventoryHost owner;

        private InventoryType inventoryType;
        private Component title;

        // Represents the player inventory slots which will be sent if present.
        // May be smaller than the player inventory (eg 9 items) and will show the player items for the rest.
        private ItemStack[] playerInventory = null;

        public InventoryWrapper(@NotNull InventoryHost owner, @NotNull InventoryType inventoryType, @NotNull ItemStack[] items, @NotNull Component title) {
            // We override handling of inventory type and title. If these values are ever observed, a mistake has been made
            super(InventoryType.CHEST_6_ROW, Component.text("unreachable"));

            this.owner = owner;
            this.inventoryType = inventoryType;
            this.title = title;

            copyInventoryContents(items);
        }

        @Override
        public @NotNull InventoryType getInventoryType() {
            return this.inventoryType;
        }

        @Override
        public @NotNull Component getTitle() {
            return this.title;
        }

        public void updateContents(@NotNull InventoryType type, @NotNull ItemStack[] items, @NotNull Component title) {
            this.inventoryType = type;
            this.title = title;
            copyInventoryContents(items);

            sendPacketToViewers(new OpenWindowPacket(getWindowId(), getInventoryType().getWindowType(), getTitle()));
            update();
            updatePlayerInventory();
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

            // If there are zero viewers we assume the inventory was closed
            if (getViewers().isEmpty()) {
                // Unmount and close
                owner.owner.unmount(owner);
            }

            return result;
        }

        private int playerInventorySlots() {
            return playerInventory == null ? 0 : playerInventory.length;
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
