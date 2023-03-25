package net.hollowcube.map.gui.hotbar;

import net.hollowcube.common.lang.LanguageProvider;
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

public final class PlayingMapHotbar {
    private PlayingMapHotbar() {}

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:map/playingmaphotbar",
                    EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, PlayingMapHotbar::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, PlayingMapHotbar::handleUseItemOnBlock);

    private static final int RETURN_CHECKPOINT_CMD = 500;
    private static final int SPECTATOR_CMD = 504;
    private static final int HUB_CMD = 508;

    private static final ItemStack RETURN_CHECKPOINT_ITEM = ItemStack.builder(Material.RED_DYE)
            .displayName(Component.translatable("gui.map.hotbar.return_checkpoint.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.map.hotbar.return_checkpoint.lore", List.of()))
            .meta(meta -> meta.customModelData(RETURN_CHECKPOINT_CMD))
            .build();

    private static final ItemStack SPECTATOR_ITEM = ItemStack.builder(Material.GREEN_DYE)
            .displayName(Component.translatable("gui.map.hotbar.spectator.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.map.hotbar.spectator.lore", List.of()))
            .meta(meta -> meta.customModelData(SPECTATOR_CMD))
            .build();

    private static final ItemStack HUB_ITEM = ItemStack.builder(Material.BLACK_DYE)
            .displayName(Component.translatable("gui.map.hotbar.hub.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.map.hotbar.hub.lore", List.of()))
            .meta(meta -> meta.customModelData(HUB_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull Player player) {
        player.getInventory().setItemStack(0, RETURN_CHECKPOINT_ITEM);
        player.getInventory().setItemStack(4, SPECTATOR_ITEM);
        player.getInventory().setItemStack(8, HUB_ITEM);
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
            case RETURN_CHECKPOINT_CMD -> player.sendMessage("todo");
            case SPECTATOR_CMD -> player.sendMessage("todo");
            case HUB_CMD -> player.sendMessage("todo");
        }
    }
}
