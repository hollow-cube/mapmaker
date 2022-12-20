package net.hollowcube.mapmaker.hub.gui.hotbar;

import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.gui.map.CreateMapsView;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HubHotbar {
    private HubHotbar() {
    }

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:hub/hotbar", EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, HubHotbar::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, HubHotbar::handleUseItemOnBlock);

    private static final int PLAY_ITEM_CMD = 500;
    private static final int CREATE_ITEM_CMD = 501;

    private static final ItemStack PLAY_MAPS_ITEM = ItemStack.builder(Material.NETHER_STAR)
            .displayName(Component.translatable("gui.hub.hotbar.play_maps.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.hub.hotbar.play_maps.lore", List.of()))
            .meta(meta -> meta.customModelData(PLAY_ITEM_CMD))
            .build();

    private static final ItemStack CREATE_MAPS_ITEM = ItemStack.builder(Material.DIAMOND_PICKAXE)
            .displayName(Component.translatable("gui.hub.hotbar.create_maps.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.hub.hotbar.create_maps.lore", List.of()))
            .meta(meta -> meta.customModelData(CREATE_ITEM_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull Player player) {
        player.getInventory().setItemStack(0, PLAY_MAPS_ITEM);
        player.getInventory().setItemStack(1, CREATE_MAPS_ITEM);
    }

    private static void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData());
    }

    private static void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData());
    }

    private static void handleItem(@NotNull Player player, int customModelData) {
        switch (customModelData) {
            case PLAY_ITEM_CMD -> {
            }
            case CREATE_ITEM_CMD -> HubServer.StaticAbuse.INSTANCE.openGUIForPlayer(player, new CreateMapsView());
        }
    }


}
