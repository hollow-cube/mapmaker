package net.hollowcube.map.gui.hotbar;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
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

public final class BuildingMapHotbar {
    private static final System.Logger logger = System.getLogger(BuildingMapHotbar.class.getName());

    private BuildingMapHotbar() {
    }

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:map/playingmaphotbar",
                    EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, BuildingMapHotbar::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, BuildingMapHotbar::handleUseItemOnBlock);

    private static final int MAP_DETAILS_CMD = 505;
    private static final int HUB_CMD = 509;

    private static final ItemStack MAP_DETAILS_ITEM = ItemStack.builder(Material.MAP)
            .displayName(Component.translatable("hotbar.map_details.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.map_details.lore", List.of()))
            .meta(meta -> meta.customModelData(MAP_DETAILS_CMD))
            .build();

    private static final ItemStack HUB_ITEM = ItemStack.builder(Material.MAGMA_CREAM)
            .displayName(Component.translatable("hotbar.regular_play.leave.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.regular_play.leave.lore", List.of()))
            .meta(meta -> meta.customModelData(HUB_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull MapWorld world, @NotNull Player player) {
        player.getInventory().setItemStack(0, MAP_DETAILS_ITEM);
        player.getInventory().setItemStack(8, HUB_ITEM);
    }

    private static void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        Thread.startVirtualThread(() -> handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData()));
    }

    private static void handleUseItemOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        Thread.startVirtualThread(() -> handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData()));
    }

    private static void handleItem(@NotNull Player player, int customModelData) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;

        switch (customModelData) {
            case HUB_CMD -> {
                try {
                    player.sendMessage("Returning to hub");

                    if (world instanceof InternalMapWorld internalWorld) {
                        internalWorld.removePlayer(player);
                    }
                    world.server().bridge().sendPlayerToHub(player);
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "failed to send player {0} to hub: {1}", player.getUuid(), e.getMessage());
                    LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                            .forEach(player::sendMessage);
                }
            }
        }
    }
}
