package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.BaseElement;
import net.hollowcube.canvas.internal.standalone.ViewContainer;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class InventoryViewHost {
    private static final Scheduler SCHEDULER = MinecraftServer.getSchedulerManager();
    private static final System.Logger logger = System.getLogger(InventoryViewHost.class.getName());

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(InventoryPreClickEvent.class, InventoryWrapper::handleInventoryClick);
    }

    private int width, height;
    private InventoryWrapper inventory = null;

    private BaseElement element = null;

    private boolean dirty = false;
    // Start in deferred mode in case the view is marked dirty immediately (eg during constructor)
//    private boolean deferredDirty = true;
    private Task redrawTask = null;

    // The number of rows of the player inventory currently in use.
    private int playerInventoryRows = 0;

    private final Deque<View> history = new ArrayDeque<>();

    private ReentrantLock clickLock = new ReentrantLock();

    public boolean canPopView() {
        return !history.isEmpty();
    }

    public void pushView(@NotNull View view, boolean isTransient) {
        if (!isTransient) history.addLast(view);
        replaceInventory((ViewContainer) view.element());
    }

    public void popView() {
        if (history.size() < 2) {
            inventory.getViewers().forEach(Player::closeInventory);
            return;
        }

        history.removeLast();
        replaceInventory((ViewContainer) history.getLast().element());
    }

    public @NotNull Inventory getHandle() {
        return inventory;
    }

    public BaseElement getElement() {
        return element;
    }

    private void replaceInventory(@NotNull ViewContainer newElement) {
        // Unmount old component if relevant
        InventoryWrapper oldInv = this.inventory;
        if (this.element != null) {
            this.element.performSignal(Element.SIG_UNMOUNT);
            // Remove inventory, do not want to handle ghost clicks
            this.inventory = null;
        }

        InventoryType type = updateSize(newElement);
        if (oldInv != null && oldInv.getInventoryType() == type && false) {
            this.inventory = oldInv; // Reuse inventory if same size, no need to send the extra packets
        } else {
            var name = oldInv == null ? Component.text("Chest") : oldInv.getTitle();
            this.inventory = new InventoryWrapper(type, name);
        }
        this.element = newElement;

        // Mount the contents in this inventory
        this.element.performSignal(Element.SIG_MOUNT);
//        deferredDirty = false;
        redrawImmediately();

        if (oldInv != null && oldInv != this.inventory) {
            // Migrate the viewers to the new inventory
            for (Player player : oldInv.getViewers()) {
                player.openInventory(inventory);
            }
        }
    }

    private @NotNull InventoryType updateSize(@NotNull ViewContainer element) {
//        Check.argCondition(section.width() > 9, "section width must be <= 9, was {}", section.width());
        var id = element.id() != null ? element.id() : "chest";

        Check.argCondition(element.width() != 9, "section width must be 9");
        Check.argCondition(element.height() > 10, "section height must be <= 10, was {}", element.height());
        width = element.width();
        height = element.height();

        // Special case for anvil GUIs
        if ("anvil".equals(id)) {
            playerInventoryRows = element.height() - 1;
            return InventoryType.ANVIL;
        }

        var inventoryType = switch (element.height()) {
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
        if (element.isConsumePlayerInventory()) {
            playerInventoryRows = 4;
        }
        return inventoryType;
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

        if (redrawTask != null) return;
        redrawTask = SCHEDULER.scheduleNextTick(this::drawCurrentElement);
    }

    public void redrawImmediately() {
        if (redrawTask != null) redrawTask.cancel();
        drawCurrentElement();
    }

    private void drawCurrentElement() {
        if (inventory == null || history.isEmpty()) return; // Inventory closed
        logger.log(System.Logger.Level.INFO, "redraw (view = {0})", history.getLast());
//        List.of(Thread.currentThread().getStackTrace())
//                .stream()
//                .map(StackTraceElement::toString)
//                .filter(s -> !s.contains("java.base/") && !s.contains("net.hollowcube.canvas"))
//                .forEach(System.out::println);


        var contents = element.getContents();
        ItemStack[] top, bottom = null;

        top = new ItemStack[getInventoryTypeSize(inventory.getInventoryType())];
        Arrays.fill(top, ItemStack.AIR);
        for (int i = 0; i < top.length; i++) {
            if (contents[i] == null) continue;
            top[i] = contents[i];
        }

        if (playerInventoryRows > 0) {
            bottom = new ItemStack[9 * playerInventoryRows];
            Arrays.fill(bottom, ItemStack.AIR);
            for (int i = 0; i < bottom.length; i++) {
                var contentIndex = getInventoryTypeSize(inventory.getInventoryType()) + i;
                if (contentIndex >= contents.length || contents[contentIndex] == null) continue;
                bottom[i] = contents[contentIndex];
            }
        }

        // Build the title string
        var titleBuilder = new FontUIBuilder();
        element.buildTitle(titleBuilder, 0, 0);

        inventory.replaceInventories(top, bottom);
//        if (inventory.getInventoryType() != InventoryType.ANVIL)
        inventory.setTitle(Component.text("", NamedTextColor.WHITE).append(titleBuilder.build()));
        dirty = false;
        redrawTask = null;
    }

    static {
        MinecraftServer.getPacketListenerManager().setPlayListener(ClientNameItemPacket.class, InventoryViewHost::handleAnvilInput);
    }

    private static void handleAnvilInput(@NotNull ClientNameItemPacket packet, @NotNull Player player) {
        if (!(player.getOpenInventory() instanceof InventoryWrapper inventory)) return;

        inventory.parent().performSignal(Element.SIG_ANVIL_INPUT, packet.itemName());
    }

    public @NotNull Player player() {
        var viewers = getHandle().getViewers().iterator();
        if (!viewers.hasNext()) throw new IllegalStateException("No viewers");
        return viewers.next();
    }

    private int getInventoryTypeSize(@NotNull InventoryType type) {
        return Math.max(type.getSize(), 9);
    }

    private class InventoryWrapper extends Inventory {

        private final ItemStack[] playerInv = new ItemStack[9 * 4];

        public InventoryWrapper(@NotNull InventoryType inventoryType, @NotNull Component title) {
            super(inventoryType, title);
            Arrays.fill(playerInv, ItemStack.AIR);
            update();
        }

        public @NotNull InventoryViewHost parent() {
            return InventoryViewHost.this;
        }

        public boolean needsPlayerInventory() {
            return playerInventoryRows > 0;
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
            if (result) {
                player.getInventory().update();
            }

            // If there are zero viewers we assume the inventory was closed
            if (getViewers().isEmpty()) {
                // Unmount and close
                element.performSignal(Element.SIG_UNMOUNT);
                element.performSignal(Element.SIG_CLOSE);
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

        private static void handleInventoryClick(@NotNull InventoryPreClickEvent event) {
            int slot = event.getSlot();
            ClickType clickType = switch (event.getClickType()) {
                case LEFT_CLICK -> ClickType.LEFT_CLICK;
                case RIGHT_CLICK -> ClickType.RIGHT_CLICK;
                case SHIFT_CLICK -> ClickType.SHIFT_LEFT_CLICK;
                default -> null;
            };
            if (clickType == null) {
                event.setCancelled(true);
                return;
            }

            // If the click happened inside the player inventory
            InventoryWrapper wrapper;
            if (event.getInventory() instanceof InventoryWrapper w) {
                wrapper = w;
            } else if (event.getInventory() == null && event.getPlayer().getOpenInventory() instanceof InventoryWrapper w) {
                // This means we are in the player inventory with a custom inventory open
                wrapper = w;
                // Offset slot to be absolute from the top of the gui
                var invSize = w.getInventoryType() == InventoryType.ANVIL ? 9 : w.getInventoryType().getSize();
                if (slot < 9) {
                    // hotbar
                    slot += invSize + (9 * 3);
                } else {
                    // main inventory
                    slot += invSize - 9;
                }
            } else {
                event.setCancelled(true);
                return;
            }

            // todo below will return post-inventory rework.
            // If the click happened inside the player inventory we need to do our weird accounting of anvil being 9 internally
//            if (slot >= wrapper.getSize()) {
//                slot -= wrapper.getSize(); // Now represents the slot starting from the bottom inventory
//                var offset = wrapper.getInventoryType() == InventoryType.ANVIL ? 9 : wrapper.getInventoryType().getSize();
//                slot += offset; // Offset to the bottom of the top inventory (accounting for our weird anvil inventory)
//            }

            var allow = wrapper.tryHandleClick(slot, event.getPlayer(), clickType);
            event.setCancelled(!allow);
        }

        private boolean tryHandleClick(int index, @NotNull Player player, @NotNull ClickType clickType) {
            if (element == null) return false;

            BaseElement.VIRTUAL_EXECUTOR.submit(() -> {
                // Prevent click while processing is still happening
                //todo this forces all into a virtual thread and i have no idea why? maybe should fix.
                // i think basically the problem boils down to tryLock locking to this particular thread
                // I probably just need a different lock implementation
                if (!clickLock.tryLock()) {
                    return; // Already handling another click, do nothing.
                }

                Future<Void> result = null;
                try {
                    result = element.handleClick(player, index, clickType);
                    if (result != null) {
                        try {
                            result.get();
                        } catch (Exception e) {
                            logger.log(System.Logger.Level.ERROR, "Failed to handle click", e);
                        } finally {
                            clickLock.unlock();
                        }
                    }
                    if (dirty) drawCurrentElement();
                } finally {
                    // If result was set to a future then the click was async and will be released later.
                    if (result == null) {
                        clickLock.unlock();
                    }
                }
            });
            return false;
        }

        private WindowItemsPacket createWindowItemsPacket(@NotNull Player player) {
            ItemStack[] convertedSlots = new ItemStack[PlayerInventory.INVENTORY_SIZE];
            Arrays.fill(convertedSlots, ItemStack.AIR);
            for (int i = 0; i < convertedSlots.length; i++) {
                var packetSlot = PlayerInventoryUtils.convertToPacketSlot(i); //minestomToProtocol
                var chestSlot = convertPlayerSlotToChestSlot(i);
                if (i < 9 * 4 && chestSlot < 9 * playerInventoryRows) {
                    convertedSlots[packetSlot] = playerInv[chestSlot];
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
        private final Player player;

        public DelegatingPlayerInventory(@NotNull Player player) {
            super(player);
            this.player = player;


            // Copy all contents of players current inventory to this one
            var old = player.getInventory();
//            viewers.addAll(old.getViewers()); todo reenable when updating minestom
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
            if (player.getOpenInventory() instanceof InventoryWrapper wrapper && wrapper.needsPlayerInventory()) {
                wrapper.updatePlayerInventory();
                return;
            }

            super.update();
        }

//        @Override
//        public void update(@NotNull Player player) {
//            // Same as above (but in case a single player update is called)
//            if (player.getOpenInventory() instanceof InventoryWrapper wrapper && wrapper.needsPlayerInventory()) {
//                wrapper.updatePlayerInventory();
//                return;
//            }
//
//            super.update(player);
//        }
    }
}
