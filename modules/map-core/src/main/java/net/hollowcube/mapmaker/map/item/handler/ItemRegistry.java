package net.hollowcube.mapmaker.map.item.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.entity.PlayerCooldown;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.InteractTarget;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.time.Cooldown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ItemRegistry is a store of all enabled items in a map.
 * <p>
 * The registry always contains all vanilla items, available in the command context.
 */
public class ItemRegistry implements PlayerCooldown {

    public static @NotNull Argument<@Nullable ItemStack> Argument(@NotNull String id) {
        return Argument.Word(id).map(
                /* mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player)) return null;
                    var world = MapWorld.forPlayerOptional(player);
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
                    var world = MapWorld.forPlayerOptional(player);
                    if (world == null) return;
                    var itemRegistry = world.itemRegistry();

                    for (var item : itemRegistry.suggestItems(raw.isBlank() ? null : raw)) {
                        //todo if the item has a description somehow we could add it to the suggestion
                        suggestion.add(item);
                    }
                }
        );
//        .errorHandler((sender, context) -> {
//            sender.sendMessage("unknown item: " + context.getRaw(word)); // todo translate
//        });
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
            .addListener(EventListener.builder(InventoryPreClickEvent.class)
                    .handler(this::handleLeftClickGui)
                    .ignoreCancelled(false)
                    .build());

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, ItemHandler> idToItemHandler = new HashMap<>();
    private final Int2ObjectMap<ItemHandler> customModelDataToItemHandler = new Int2ObjectArrayMap<>();
    private final Int2ObjectMap<ItemHandler> materialToItemHandler = new Int2ObjectArrayMap<>();

    // Contains all the "public" item names known by this registry. Used for completions.
    private final Set<NamespaceID> allItemNames = new TreeSet<>((a, b) -> a.asString().compareToIgnoreCase(b.asString()));

    public ItemRegistry() {
        for (var item : Material.values()) {
            allItemNames.add(item.namespace());
        }

//        register(DebugStickItem.INSTANCE); //todo
    }

    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public void register(@NotNull ItemHandler itemHandler) {
        try {
            lock.lock();
            idToItemHandler.put(itemHandler.id().asString().toLowerCase(Locale.ROOT), itemHandler);
            if (itemHandler.customModelData() != -1) {
                customModelDataToItemHandler.put(itemHandler.customModelData(), itemHandler);
            } else {
                materialToItemHandler.put(itemHandler.material().id(), itemHandler);
            }

            allItemNames.add(itemHandler.id());
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
            idToItemHandler.put(itemHandler.id().asString().toLowerCase(Locale.ROOT), itemHandler);
            if (itemHandler.customModelData() != -1) {
                customModelDataToItemHandler.put(itemHandler.customModelData(), itemHandler);
            } else {
                materialToItemHandler.put(itemHandler.material().id(), itemHandler);
            }
        } finally {
            lock.unlock();
        }
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull NamespaceID id, @Nullable CompoundBinaryTag nbt) {
        return getItemStack(id.asString(), nbt);
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull String id, @Nullable CompoundBinaryTag nbt) {
        var namespace = id.toLowerCase(Locale.ROOT);

        var itemHandler = idToItemHandler.get(namespace);
        if (itemHandler != null) {
            return itemHandler.buildItemStack(nbt);
        }

        var material = Material.fromNamespaceId(namespace);
        if (material == null) return null;
        var builder = ItemStack.builder(material);
        return builder.build();
    }

    public @Nullable String getItemId(@NotNull ItemStack itemStack) {
        var itemHandler = getHandlerFromItemStack(itemStack);
        return itemHandler == null ? null : itemHandler.id().asString();
    }

    private @NotNull List<String> suggestItems(@Nullable String filter) {
        var normalFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        return allItemNames.stream()
                .filter(name -> name.asString().startsWith(normalFilter)
                        || name.path().startsWith(normalFilter))
                .limit(20)
                .map(NamespaceID::asString)
                .toList();
    }

    private void handleInstanceTick(@NotNull InstanceTickEvent event) {
        for (var player : event.getInstance().getPlayers()) {
            player.removeTag(TRIGGER_TAG);
        }
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != Player.Hand.MAIN) return;

        var itemHandler = getHandlerFromItemStack(event.getItemStack());
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_AIR)) return;

        // For some dumb reason this is triggered when right clicking on a block,
        // so is playerUseItemOnBlockEvent so we filter this one out.
        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;
        tryUseCooldown(player, () -> {
            player.setTag(TRIGGER_TAG, true);
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler,
                    player,
                    event.getItemStack(),
                    event.getHand(),
                    null, null,
                    null, null
            ));
        });

        event.setCancelled(true);
    }

    private void handleUseItemOnBlock(@NotNull PlayerBlockInteractEvent event) {
        if (event.isCancelled() || event.isBlockingItemUse()) return;
        if (event.getHand() != Player.Hand.MAIN) return;

        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        // This is a somewhat weird special case to allow right clicking a checkpoint with a checkpoint. But oh well.
        if (!player.isSneaking() && event.getBlock().handler() instanceof InteractTarget)
            return;

        var itemStack = player.getItemInHand(event.getHand());
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;
        tryUseCooldown(player, () -> {
            System.out.println("place the block item");
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
        });

        event.setBlockingItemUse(true);
    }

    private void handleUseItemOnEntity(@NotNull PlayerEntityInteractEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;

        var itemStack = player.getItemInHand(event.getHand());
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_ENTITY)) return;
        tryUseCooldown(player, () -> {
            player.setTag(TRIGGER_TAG, true);
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler, player,
                    itemStack, event.getHand(),
                    null,
                    null,
                    null,
                    event.getTarget()
            ));
        });
    }

    private void handlePlaceBlock(@NotNull PlayerBlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (event.getHand() != Player.Hand.MAIN) return;

        var itemStack = event.getPlayer().getItemInHand(event.getHand());
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;
        tryUseCooldown(event.getPlayer(), () -> {
            var placeOffset = event.getBlockFace().toDirection();
            itemHandler.rightClicked(new ItemHandler.Click(
                    itemHandler,
                    event.getPlayer(),
                    itemStack,
                    event.getHand(),
                    event.getBlockPosition().sub(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                    event.getBlockPosition(),
                    event.getBlockFace(),
                    null
            ));
        });

        event.setCancelled(true);
    }

    private void handleBreakBlock(@NotNull PlayerBlockBreakEvent event) {
        if (event.isCancelled()) return;
        var itemStack = event.getPlayer().getItemInHand(Player.Hand.MAIN);
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.LEFT_CLICK_BLOCK)) return;
        tryUseCooldown(event.getPlayer(), () -> itemHandler.leftClicked(new ItemHandler.Click(
                itemHandler,
                event.getPlayer(),
                itemStack,
                Player.Hand.MAIN,
                event.getBlockPosition(),
                null,
                event.getBlockFace(),
                null
        )));

        event.setCancelled(true);
    }

    private void handleHitEntity(@NotNull EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getTag(TRIGGER_TAG) != null) return;

        var itemStack = player.getItemInMainHand();
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.LEFT_CLICK_ENTITY)) return;

        player.setTag(TRIGGER_TAG, true);
        itemHandler.leftClicked(new ItemHandler.Click(
                itemHandler, player,
                itemStack, Player.Hand.MAIN,
                null,
                null,
                null,
                event.getTarget()
        ));
    }

    private void handleLeftClickGui(@NotNull InventoryPreClickEvent event) {
        var player = event.getPlayer();
        if (player.getTag(TRIGGER_TAG) != null) return;
        //todo support other gui actions here
        if (event.getInventory() != null || event.getClickType() != ClickType.LEFT_CLICK) return;

        var itemStack = event.getClickedItem();
        var itemHandler = getHandlerFromItemStack(itemStack);
        if (itemHandler == null || !itemHandler.allows(ItemHandler.LEFT_CLICK_GUI)) return;

        player.setTag(TRIGGER_TAG, true);
        itemHandler.leftClicked(new ItemHandler.Click(
                itemHandler, player, itemStack, Player.Hand.MAIN,
                null, null, null, null
        ));
    }

    private @Nullable ItemHandler getHandlerFromItemStack(@NotNull ItemStack itemStack) {
        var itemHandler = materialToItemHandler.get(itemStack.material().id());
        if (itemHandler != null) return itemHandler;
        return customModelDataToItemHandler.get(itemStack.get(ItemComponent.CUSTOM_MODEL_DATA, -1));
    }

    private static final Tag<Cooldown> COOLDOWN_TAG = Tag.Transient("mapmaker:hotbar_cooldown");

    private static final Duration COOLDOWN_TIME = Duration.of(250, ChronoUnit.MILLIS);

    @Override
    public @NotNull Tag<Cooldown> cooldownTag() {
        return COOLDOWN_TAG;
    }

    @Override
    public @NotNull Duration cooldownDuration() {
        return COOLDOWN_TIME;
    }
}
