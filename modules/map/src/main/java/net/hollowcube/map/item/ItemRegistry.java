package net.hollowcube.map.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.terraform.tool.BuiltinTool;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ItemRegistry is a store of all enabled items in a map.
 * <p>
 * The registry always contains all vanilla items, available in the command context.
 */
public class ItemRegistry {

    public static @NotNull Argument<@Nullable ItemStack> Argument(@NotNull String id) {
        var word = Argument.Word(id);
        return word.map(
                /* mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player)) return null;
                    var world = MapWorld.forPlayerOptional(player);
                    if (world == null) return null;
                    var itemRegistry = world.itemRegistry();

                    var suggestions = itemRegistry.suggestItems(raw);
                    if (suggestions.isEmpty()) return new Argument.ParseFailure<>();
                    if (suggestions.size() > 1) {
                        // Look for exact match and succeed, otherwise fail if we have more than one suggestion and we aren't sure
                        ItemStack stack = itemRegistry.getItemStack(raw, null);
                        if (stack != null) {
                            return new Argument.ParseSuccess<>(stack);
                        } else {
                            return new Argument.ParsePartial<>();
                        }
                    }
                    return new Argument.ParseSuccess<>(itemRegistry.getItemStack(suggestions.get(0), null));
                },
                /* suggester */ (sender, reader, suggestion, raw) -> {
                    if (!(sender instanceof Player player)) return;
                    var world = MapWorld.forPlayerOptional(player);
                    if (world == null) return;
                    var itemRegistry = world.itemRegistry();

                    for (var item : itemRegistry.suggestItems(raw.isBlank() ? null : raw)) {
                        //todo if the item has a description somehow we could add it to the suggestion
                        suggestion.add(item);
                    }
                }
        ).errorHandler((sender, context) -> {
            sender.sendMessage("unknown item: " + context.getRaw(word)); // todo translate
        });
    }

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:item/registry", EventFilter.INSTANCE)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleUseItemOnBlock)
            .addListener(PlayerUseItemEvent.class, this::handleUseItem)
            .addListener(PlayerBlockPlaceEvent.class, this::handlePlaceBlock);

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, ItemHandler> idToItemHandler = new HashMap<>();
    private final Int2ObjectMap<ItemHandler> customModelDataToItemHandler = new Int2ObjectArrayMap<>();

    // Contains all the "public" item names known by this registry. Used for completions.
    private final Set<NamespaceID> allItemNames = new TreeSet<>((a, b) -> a.asString().compareToIgnoreCase(b.asString()));

    public ItemRegistry() {
        for (var item : Material.values()) {
            allItemNames.add(item.namespace());
        }
    }

    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public void register(@NotNull ItemHandler itemHandler) {
        try {
            lock.lock();
            idToItemHandler.put(itemHandler.id().asString().toLowerCase(Locale.ROOT), itemHandler);
            customModelDataToItemHandler.put(itemHandler.customModelData(), itemHandler); // NOSONAR - It thinks put is deprecated
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
            customModelDataToItemHandler.put(itemHandler.customModelData(), itemHandler); // NOSONAR - It thinks put is deprecated
        } finally {
            lock.unlock();
        }
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull NamespaceID id, @Nullable NBTCompound nbt) {
        return getItemStack(id.asString(), nbt);
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull String id, @Nullable NBTCompound nbt) {
        var namespace = id.toLowerCase(Locale.ROOT);

        var itemHandler = idToItemHandler.get(namespace);
        if (itemHandler != null) {
            return itemHandler.buildItemStack(nbt);
        }

        var material = Material.fromNamespaceId(namespace);
        if (material == null) return null;
        var builder = ItemStack.builder(material);
        if (material == Material.DEBUG_STICK)
            builder.setTag(BuiltinTool.TYPE, "terraform:debug_stick");
        return builder.build();
    }

    private @NotNull List<String> suggestItems(@Nullable String filter) {
        var normalFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        System.out.println(normalFilter);
        return allItemNames.stream()
                .filter(name -> name.asString().startsWith(normalFilter)
                        || name.path().startsWith(normalFilter))
                .limit(20)
                .map(NamespaceID::asString)
                .toList();
    }

    private void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var cmd = event.getItemStack().meta().getCustomModelData();
        var itemHandler = customModelDataToItemHandler.get(cmd);
        if (itemHandler == null) return;

        if (!itemHandler.allows(ItemHandler.RIGHT_CLICK_AIR)) return;

        var player = event.getPlayer();
        if (player.getTargetBlockPosition(5) != null) {
            // For some dumb reason this is triggered when right clicking on a block, so is playerUseItemOnBlockEvent
            // so we filter this one out.
            return;
        }

        event.setCancelled(true);
        itemHandler.rightClicked(new ItemHandler.Click(
                itemHandler,
                player,
                event.getItemStack(),
                event.getHand(),
                null, null,
                null, null
        ));
    }

    private void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var cmd = event.getItemStack().meta().getCustomModelData();
        var itemHandler = customModelDataToItemHandler.get(cmd);
        if (itemHandler == null) return;

        if (!itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;

        var placeOffset = event.getBlockFace().toDirection();
        itemHandler.rightClicked(new ItemHandler.Click(
                itemHandler,
                event.getPlayer(),
                event.getItemStack(),
                event.getHand(),
                event.getPosition(),
                event.getPosition().add(placeOffset.normalX(), placeOffset.normalY(), placeOffset.normalZ()),
                event.getBlockFace(),
                null
        ));
    }

    private void handlePlaceBlock(@NotNull PlayerBlockPlaceEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;

        var itemStack = event.getPlayer().getItemInHand(event.getHand());
        var cmd = itemStack.meta().getCustomModelData();
        var itemHandler = customModelDataToItemHandler.get(cmd);
        if (itemHandler == null) return;

        event.setCancelled(true);
        if (!itemHandler.allows(ItemHandler.RIGHT_CLICK_BLOCK)) return;

        event.setCancelled(true);
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
    }

}
