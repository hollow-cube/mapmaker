package net.hollowcube.mapmaker.map.item.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.Map2PlayerBlockInteractEvent;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.ItemBlockState;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ItemRegistry is a store of all enabled items in a map.
 * <p>
 * The registry always contains all vanilla items, available in the command context.
 */
public class ItemRegistry {

    public static @NotNull Argument<@Nullable ItemStack> Argument(@NotNull String id) {
        return Argument.Word(id).map(
                /* mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player)) return null;
                    var world = MapWorld.forPlayer(player);
                    if (world == null) return null;
                    var itemRegistry = world.itemRegistry();

                    var suggestions = itemRegistry.suggestItems(raw);
                    if (suggestions.isEmpty()) return new ParseResult.Failure<>(-1);
                    if (suggestions.size() > 1) {
                        // Look for exact match and succeed, otherwise fail if we have more than one suggestion and we aren't sure
                        ItemStack stack = itemRegistry.getItemStack(raw, null);
                        if (stack != null) {
                            return new ParseResult.Success<>(stack);
                        } else {
                            return new ParseResult.Partial<>();
                        }
                    }
                    return new ParseResult.Success<>(itemRegistry.getItemStack(suggestions.get(0), null));
                },
                /* suggester */ (sender, raw, suggestion) -> {
                    if (!(sender instanceof Player player)) return;
                    var world = MapWorld.forPlayer(player);
                    if (world == null) return;
                    var itemRegistry = world.itemRegistry();

                    for (var item : itemRegistry.suggestItems(raw.isBlank() ? null : raw)) {
                        //todo if the item has a description somehow we could add it to the suggestion
                        suggestion.add(item);
                    }
                }
        );
    }

    private static final Tag<Boolean> TRIGGER_TAG = Tag.Transient("mapmaker:triggered");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:item/registry", EventFilter.INSTANCE)
            .addListener(PlayerBlockInteractEvent.class, this::handleUseItemOnBlock)
            .addListener(PlayerUseItemEvent.class, this::handleUseItem)
            .addListener(PlayerBlockPlaceEvent.class, this::handlePlaceBlock)
            .addListener(PlayerBlockBreakEvent.class, this::handleBreakBlock)
            .addListener(PlayerEntityInteractEvent.class, this::handleUseItemOnEntity)
            .addListener(EntityAttackEvent.class, this::handleHitEntity)
            .addListener(InstanceTickEvent.class, this::handleInstanceTick)
            .addListener(PlayerBeginItemUseEvent.class, this::handleBeginItemUse)
            .addListener(PlayerCancelItemUseEvent.class, this::handleCancelItemUse)
            .addListener(EventListener.builder(InventoryPreClickEvent.class)
                    .handler(this::handleLeftClickGui)
                    .ignoreCancelled(false)
                    .build());

    private final ReentrantLock lock = new ReentrantLock();

    private final Map<String, ItemHandler> idToItemHandler = new HashMap<>();
    private final Int2ObjectMap<ItemHandler> blockToItemHandler = new Int2ObjectOpenHashMap<>();
    private final Set<Key> publicItems = new TreeSet<>((a, b) -> a.asString().compareToIgnoreCase(b.asString()));

    private final TagCooldown useCooldown = new TagCooldown("mapmaker:hotbar_cooldown", 100);

    public ItemRegistry() {
        for (var item : Material.values()) {
            publicItems.add(item.key());
        }
    }

    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public void register(@NotNull ItemHandler handle) {
        try {
            lock.lock();
            idToItemHandler.put(handle.key().asString().toLowerCase(Locale.ROOT), handle);
            if (handle instanceof BlockItemHandler blockItemHandler) {
                blockToItemHandler.put(blockItemHandler.block().id(), handle);
            }

            publicItems.add(handle.key());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers the item handler but without allowing it to be used in commands.
     */
    public void registerSilent(@NotNull ItemHandler itemHandler) {
        //todo this is still allowing /give to work, need to fix.
        try {
            lock.lock();
            idToItemHandler.put(itemHandler.key().asString().toLowerCase(Locale.ROOT), itemHandler);
        } finally {
            lock.unlock();
        }
    }

    public boolean setItemStack(@NotNull Player player, @NotNull Key id, int slot) {
        return setItemStack(player, id, slot, null);
    }

    public boolean setItemStack(@NotNull Player player, @NotNull Key id, int slot, @Nullable CompoundBinaryTag nbt) {
        var itemStack = getItemStack(id, nbt);
        if (itemStack == null) return false;

        player.getInventory().setItemStack(slot, itemStack);
        return true;
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull Key id, @Nullable CompoundBinaryTag nbt) {
        return getItemStack(id.asString(), nbt);
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull String id, @Nullable CompoundBinaryTag nbt) {
        var namespace = id.toLowerCase(Locale.ROOT);

        var itemHandler = idToItemHandler.get(namespace);
        if (itemHandler != null) {
            return itemHandler.getItemStack(nbt);
        }

        var material = Material.fromKey(namespace);
        if (material == null) return null;
        var builder = ItemStack.builder(material);
        return builder.build();
    }

    public @Nullable ItemStack getItemStack(@NotNull Block block, boolean includeData) {
        if (block.handler() == null) return null;

        var itemHandler = blockToItemHandler.get(block.id());
        if (itemHandler == null) return null;
        // Should ignore if this does not have the expected handler. For example a regular pressure plate.
        if (!(itemHandler instanceof BlockItemHandler bih) || !Objects.equals(bih.block().handler().getKey(), block.handler().getKey()))
            return null;

        var itemStack = itemHandler.getItemStack(includeData ? block.nbt() : null);
        if (includeData && !block.properties().isEmpty()) {
            itemStack = itemStack.with(DataComponents.BLOCK_STATE, new ItemBlockState(block.properties()));
        }

        return itemStack;
    }

    public @Nullable String getItemId(@NotNull Player player, int slot) {
        return getItemId(player.getInventory().getItemStack(slot));
    }

    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        var itemHandler = getHandlerFromItemStack(itemStack);
        return itemHandler == null ? null : itemHandler.key().asString();
    }

    /**
     * Gets an itemstack for the given itemstack, returning the same stack if it does not have a handler.
     * But if it does have a handler will return a new stack as to make sure things like the lore and display name are correct.
     */
    public ItemStack getItemStack(@NotNull ItemStack itemStack) {
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null) return itemStack;
        return itemHandler.getItemStack();
    }

    public List<ItemStack> getCustomPublicItemStacks() {
        return this.idToItemHandler
            .keySet()
            .stream()
            .map(Key::key)
            .filter(this.publicItems::contains)
            .map(it -> getItemStack(it, null))
            .toList();
    }

    private @NotNull List<String> suggestItems(@Nullable String filter) {
        var normalFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return publicItems.stream()
                .filter(name -> name.asString().startsWith(normalFilter)
                        || name.value().startsWith(normalFilter))
                .limit(20)
                .map(Key::asString)
                .toList();
    }

    private void handleInstanceTick(@NotNull InstanceTickEvent event) {
        for (var player : event.getInstance().getPlayers()) {
            player.removeTag(TRIGGER_TAG);
        }
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != PlayerHand.MAIN) return;

        var itemHandler = getHandlerFromItemStack(event.getItemStack());
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_AIR)) return;

        // For some dumb reason this is triggered when right clicking on a block,
        // so is playerUseItemOnBlockEvent so we filter this one out.
        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        if (useCooldown.test(player) && player instanceof MapPlayer mp && mp.tryUseItem(event.getItemStack())) {
            player.setTag(TRIGGER_TAG, true);
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler,
                    player,
                    event.getItemStack(),
                    event.getHand(),
                    null, null,
                    null, null
            ));
        }

        event.setCancelled(true);
    }

    private void handleUseItemOnBlock(@NotNull PlayerBlockInteractEvent event) {
        if (event.isCancelled() || event.isBlockingItemUse()) return;
        if (event.getHand() != PlayerHand.MAIN) return;

        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        // See note on the event for why this exists :(
        var world = MapWorld.forPlayer(player);
        if (world != null && !player.isSneaking() && event.getBlock().handler() != null) {
            var newEvent = new Map2PlayerBlockInteractEvent(world, player, event.getBlock(), event.getBlockPosition(), event.getHand());
            world.callEvent(newEvent);
            if (newEvent.isCancelled()) return;
        }

        var itemStack = player.getItemInHand(event.getHand());
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;

        if (useCooldown.test(player) && player instanceof MapPlayer mp && mp.tryUseItem(itemStack)) {
            player.setTag(TRIGGER_TAG, true);
            var placeOffset = event.getBlockFace().toDirection();
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler, player,
                    itemStack, event.getHand(),
                    event.getBlockPosition(),
                    event.getBlockPosition().add(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                    event.getBlockFace(),
                    null
            ));
        }

        event.setBlockingItemUse(true);
    }

    private void handleUseItemOnEntity(@NotNull PlayerEntityInteractEvent event) {
        if (event.getHand() != PlayerHand.MAIN) return;

        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        var itemStack = player.getItemInHand(event.getHand());
        var handler = getHandlerFromItemStack(itemStack);
        if (handler == null || !handler.allows(ItemHandler.RIGHT_CLICK_ENTITY)) return;

        if (useCooldown.test(player) && player instanceof MapPlayer mp && mp.tryUseItem(itemStack)) {
            player.setTag(TRIGGER_TAG, true);
            handler.rightClicked(new ItemHandler.Click(handler, player, itemStack, event.getHand(), event.getTarget()));
        }
    }

    private void handlePlaceBlock(@NotNull PlayerBlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != PlayerHand.MAIN) return;

        var player = event.getPlayer();
        var itemStack = player.getItemInHand(event.getHand());
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;

        if (useCooldown.test(player)) {
            var placeOffset = event.getBlockFace().toDirection();
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler,
                    player,
                    itemStack,
                    event.getHand(),
                    event.getBlockPosition().sub(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                    event.getBlockPosition(),
                    event.getBlockFace(),
                    null
            ));
        }

        event.setCancelled(true);
    }

    private void handleBreakBlock(@NotNull PlayerBlockBreakEvent event) {
        if (event.isCancelled()) return;

        var player = event.getPlayer();
        var itemStack = player.getItemInHand(PlayerHand.MAIN);
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.LEFT_CLICK_BLOCK)) return;

        if (useCooldown.test(player)) {
            itemHandler.leftClicked(new ItemHandler.Click(
                    itemHandler,
                    event.getPlayer(),
                    itemStack,
                    PlayerHand.MAIN,
                    event.getBlockPosition(),
                    null,
                    event.getBlockFace(),
                    null
            ));
        }

        event.setCancelled(true);
    }

    private void handleHitEntity(@NotNull EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getTag(TRIGGER_TAG) != null) return;

        var itemStack = player.getItemInMainHand();
        var handler = getHandlerFromItemStack(itemStack);
        if (handler == null || !handler.allows(ItemHandler.LEFT_CLICK_ENTITY)) return;

        player.setTag(TRIGGER_TAG, true);
        handler.leftClicked(new ItemHandler.Click(handler, player, itemStack, PlayerHand.MAIN, event.getTarget()));
    }

    private void handleLeftClickGui(@NotNull InventoryPreClickEvent event) {
        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        // TODO(1.21.4)

        //todo support other gui actions here
//        if (event.getInventory() != null || event.getClickType() != ClickType.LEFT_CLICK) return;
//
//        var itemStack = event.getClickedItem();
//        var itemHandler = getHandlerFromItemStack(itemStack);
//        if (itemHandler == null || !itemHandler.allows(ItemHandler.LEFT_CLICK_GUI)) return;
//
//        player.setTag(TRIGGER_TAG, true);
//        itemHandler.leftClicked(new ItemHandler.Click(
//                itemHandler, player, itemStack, PlayerHand.MAIN,
//                null, null, null, null
//        ));
    }

    private void handleBeginItemUse(@NotNull PlayerBeginItemUseEvent event) {
        var player = event.getPlayer();
        var itemStack = player.getItemInMainHand();
        var handler = getHandlerFromItemStack(itemStack);
        if (handler == null || !handler.allows(ItemHandler.CONSUME_ITEM)) return;

        int duration = handler.beginConsume(new ItemHandler.Click(handler, player, itemStack,
                PlayerHand.MAIN, null, null, null, null));
        if (duration < 0) event.setCancelled(true);
        else event.setItemUseDuration(duration);
    }

    private void handleCancelItemUse(@NotNull PlayerCancelItemUseEvent event) {
        var player = event.getPlayer();
        var itemStack = player.getItemInMainHand();
        var handler = getHandlerFromItemStack(itemStack);
        if (handler == null || !handler.allows(ItemHandler.CONSUME_ITEM)) return;

        var click = new ItemHandler.Click(handler, player, itemStack,
                PlayerHand.MAIN, null, null, null, null);
        var result = handler.cancelConsume(click, event.getUseDuration());
        event.setRiptideSpinAttack(result == ItemHandler.ConsumeItemResult.RIPTIDE_SPIN);
    }

    private @Nullable ItemHandler getHandlerFromItemStack(@NotNull ItemStack itemStack) {
        var id = Objects.requireNonNullElseGet(itemStack.getTag(ItemHandler.ID_TAG),
                () -> itemStack.material().key().asString());
        return idToItemHandler.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean isOnCooldown(@NotNull Player player) {
        return !useCooldown.test(player);
    }
}
