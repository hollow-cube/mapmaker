package net.hollowcube.map.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.terraform.tool.BuiltinTool;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.TagHandler;
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

    public static @NotNull Argument<@Nullable String> Argument(@NotNull String id) {
        return ArgumentType.ResourceLocation(id)
                .setSuggestionCallback((sender, context, suggestion) -> {
                    if (!(sender instanceof Player player)) return;
                    var mapWorld = MapWorld.forPlayerOptional(player);
                    if (mapWorld == null) return;
                    var itemRegistry = mapWorld.itemRegistry();

                    var input = suggestion.getInput().substring(suggestion.getStart() - 1).trim();
                    for (var item : itemRegistry.suggestItems(input.isBlank() ? null : input)) {
                        //todo if the item has a description somehow we could add it to the suggestion
                        suggestion.addEntry(new SuggestionEntry(item));
                    }
                });
//                .map((sender, value) -> {
//                    if (!(sender instanceof Player player)) return null;
//                    var mapWorld = MapWorld.optionalFromInstance(player.getInstance());
//                    if (mapWorld == null) return null;
//                    var itemRegistry = mapWorld.itemRegistry();
//
//                    return itemRegistry.parseItem(value);
//                });
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

    public @UnknownNullability ItemStack getItemStack(@NotNull NamespaceID id, @Nullable NBTCompound nbt) {
        return getItemStack(id.asString(), nbt);
    }

    public @UnknownNullability ItemStack getItemStack(@NotNull String id, @Nullable NBTCompound nbt) {
        var namespace = id.toLowerCase(Locale.ROOT);

        var itemHandler = idToItemHandler.get(namespace);
        if (itemHandler != null) {
            var builder = ItemStack.builder(itemHandler.material());
            var baseTranslationKey = String.format("item.%s.%s",
                    itemHandler.id().namespace(), itemHandler.id().path());
            builder.displayName(Component.translatable(baseTranslationKey + ".name"));
            builder.lore(LanguageProvider.optionalMultiTranslatable(baseTranslationKey + ".lore", List.of()));
            itemHandler.updateItemStack(builder, TagHandler.newHandler()); //todo pass nbt
            builder.meta(meta -> meta.customModelData(itemHandler.customModelData()));
            return builder.build();
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
