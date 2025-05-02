package net.hollowcube.mapmaker.scripting.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.mapmaker.scripting.annotation.ScriptSafe;
import net.hollowcube.mapmaker.scripting.gui.node.Node;
import net.hollowcube.mapmaker.scripting.gui.util.ClickType;
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
import net.minestom.server.network.packet.server.play.OpenWindowPacket;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.validate.Check;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

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

    Value reconcilerRoot; // Cache for gui manager, should not be modified here.
    private final GuiManager owner;
    private final Player player;

    private final InventoryWrapper handle = new InventoryWrapper();

    // One root corresponds to one element. Only the top/last root is rendered.
    private final List<Object> elements = new ArrayList<>(); // React elements (wrapped with a keyed fragment)
    private final List<InventoryType> inventoryTypes = new ArrayList<>(); // Inventory types (same as elements)
    private final List<Node> roots = new ArrayList<>(); // Layout nodes

    // Microtasks scheduled by react which will be executed when we exit the JS context.
    // TODO: We could be fancy and choose to wait a tick to execute these if there is not enough time left in the tick.
    private final Queue<Value> pendingMicrotasks = new ArrayDeque<>();
    private final Int2ObjectMap<Task> pendingTasks = new Int2ObjectArrayMap<>();
    private int nextTaskId = 1;
    private Task redrawTask = null;
    // If present, indicates a potentially pending click event. We should not respond to other clicks while this
    // is both not null and not CompletableFuture#isDone.
    private CompletableFuture<Void> pendingClick = null;

    private boolean hasMounted = false;

    InventoryHost(@NotNull GuiManager owner, @NotNull Player player) {
        this.owner = owner;
        this.player = player;
    }

    public @NotNull Inventory handle() {
        return Objects.requireNonNull(handle);
    }

    public interface JsContext extends AutoCloseable {
        @Override void close();
    }

    public @NotNull JsContext jsEnter() {
        if (CURRENT.get() != null)
            return () -> {
            };
        CURRENT.set(this);
        return this::jsExit;
    }

    private void jsExit() {
        try {
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
        } finally {
            CURRENT.remove();
        }
    }

    @ScriptSafe
    public void scheduleMicrotask(@NotNull Value microtask) {
        if (!microtask.canExecute()) throw new IllegalArgumentException("Microtask must be a function");
        pendingMicrotasks.add(microtask);
    }

    @ScriptSafe
    public int scheduleTask(@NotNull Value task, int delayMillis, @NotNull Value[] args) {
        if (!task.canExecute()) throw new IllegalArgumentException("Task must be a function");

        final int taskId = nextTaskId++;
        Runnable taskImpl = () -> {
            if (!pendingTasks.containsKey(taskId)) return; // Task was cancelled
            try (var _ = jsEnter()) {
                task.executeVoid((Object[]) args);
            } catch (Exception e) {
                logger.error("Failed to execute task", e);
                PostHog.captureException(e, player.getUuid().toString());
            } finally {
                // Remove the task from the pending tasks map
                this.pendingTasks.remove(taskId);
            }
        };

        this.pendingTasks.put(taskId, player.scheduler().buildTask(taskImpl)
                .delay(TaskSchedule.millis(delayMillis)).schedule());
        return taskId;
    }

    @ScriptSafe
    public void cancelTask(int taskId) {
        final Task task = this.pendingTasks.remove(taskId);
        if (task != null) task.cancel();
    }

    @ScriptSafe
    public void pushView(@NotNull Value reactElement) {
        final InventoryType inventoryType = owner.getInventoryType(reactElement);

        // We overwrite the key to always be a random UUID.
        // This is because we always keep the entire view stack mounted in the React root to preserve
        // state when moving forward and backward.
        // But when rerendering we don't want to remount the entire tree, so we set a constant
        // key for each view in the tree.
        Object elem = reactElement;
        if (owner.engine().env().isDevelopment()) {
            // To add some extra trickery, react-development will freeze the result of createElement,
            // so we copy it to change the key.
            final Map<String, Object> elementCopy = new HashMap<>();
            for (final String key : reactElement.getMemberKeys()) {
                if ("key".equals(key)) continue;
                elementCopy.put(key, reactElement.getMember(key));
            }
            elementCopy.put("key", UUID.randomUUID().toString());
            elem = proxyObject(elementCopy);
        } else {
            // In production we can just set the key directly.
            reactElement.putMember("key", UUID.randomUUID().toString());
        }

        elements.add(elem);
        inventoryTypes.add(inventoryType);

        updateAndDraw();
    }

    @ScriptSafe
    public void popView() {
        if (elements.size() <= 1) return;

        // Remove the react element
        this.elements.removeLast();
        this.inventoryTypes.removeLast();

        updateAndDraw();
    }

    @ScriptSafe
    private void updateAndDraw() {
        // Perform a react update
        try (var _ = jsEnter()) {
            var rootElement = owner.reactCreateFragment(proxyObject(Map.of()), elements);
            owner.updateContainer(this, rootElement);
        }

        // Queue a redraw (or initial draw) at the end of the tick
        queueRedraw();
    }

    public void queueRedraw() {
        if (this.redrawTask != null && this.redrawTask.isAlive()) return;
        this.redrawTask = player.scheduler().scheduleEndOfTick(this::drawCurrentElement);
    }

    // React-reconciler interactions

    public void addChild(@NotNull Node node) {
        this.roots.add(node);
        drawCurrentElement(); // todo do we need this call? it seems bad
    }

    public void removeChild(@NotNull Node child) {
        var index = this.roots.indexOf(child);
        if (index == -1) throw new IllegalStateException("Child not found: " + child);

        this.roots.remove(index);
    }

    public void clear() {
        this.roots.clear();
    }

    // "rendering" implementation

    private void drawCurrentElement() {
        // Check if unmounted or closed
        if (this.roots.isEmpty() || (hasMounted && !(player.getOpenInventory() instanceof InventoryWrapper))) return;

        final Node root = this.roots.getLast();
        final InventoryType type = this.inventoryTypes.getLast();

        // Currently we always consume the player inventory so add 4 rows.
        int containerSizeInRows = getInterpretedSize(type) / 9;
        var menuBuilder = new MenuBuilder(9, containerSizeInRows + 4, containerSizeInRows);
        root.build(menuBuilder);

        this.handle.updateContents(type, menuBuilder.getItems(), menuBuilder.getTitle());
        if (!handle.isViewer(player)) {
            player.openInventory(handle);
            hasMounted = true;
        }
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

        if (host.roots.isEmpty()) return; // Check for unmounted.
        final Node root = host.roots.getLast();

        // In case we are already handling a click, we should not accept another one.
        if (host.pendingClick != null && !host.pendingClick.isDone()) {
            return;
        }

        // At this point we always consume the click
        event.setCancelled(true);
        final ClickType clickType = switch (event.getClick()) {
            case Click.Left ignored -> ClickType.LEFT;
            case Click.Right ignored -> ClickType.RIGHT;
            case Click.LeftShift ignored -> ClickType.LEFT_SHIFT;
            case Click.RightShift ignored -> ClickType.RIGHT_SHIFT;
            default -> null;
        };
        if (clickType == null) return;

        try (var _ = host.jsEnter()) {
            host.pendingClick = root.handleClick(clickType, slot % 9, slot / 9);
            if (host.pendingClick != null) {
                host.player.playSound(CLICK_SOUND);
            }
        } catch (Exception e) {
            logger.error("Failed to handle click", e);
            PostHog.captureException(e, host.player.getUuid().toString());
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
        // TODO
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
                try (var _ = jsEnter()) {
                    owner.updateContainer(InventoryHost.this, null); // Unmount
                }
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
