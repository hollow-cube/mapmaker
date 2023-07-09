package net.hollowcube.map.gui.hotbar;

import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
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
    private static final System.Logger logger = System.getLogger(PlayingMapHotbar.class.getName());

    private PlayingMapHotbar() {
    }

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:map/playingmaphotbar",
                    EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, PlayingMapHotbar::handleUseItem)
            .addListener(PlayerUseItemOnBlockEvent.class, PlayingMapHotbar::handleUseItemOnBlock);

    private static final int RESET_CMD = 508;
    private static final int HUB_CMD = 509;
    private static final ItemStack RESET_ITEM = ItemStack.builder(Material.REDSTONE)
            .displayName(Component.translatable("hotbar.regular_play.reset.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("hotbar.regular_play.reset.lore", List.of()))
            .meta(meta -> meta.customModelData(RESET_CMD))
            .build();

    private static final ItemStack HUB_ITEM = ItemStack.builder(Material.REDSTONE)
            .displayName(Component.translatable("hotbar.regular_play.leave.name"))
            .lore(LanguageProvider.optionalMultiTranslatable("hotbar.regular_play.leave.lore", List.of()))
            .meta(meta -> meta.customModelData(HUB_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull Player player) {
        player.getInventory().setItemStack(7, RESET_ITEM);
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
            case RESET_CMD -> {
                //todo add this in a common place and a /reset command
                var mapService = world.server().mapService();
                var playerData = PlayerDataV2.fromPlayer(player);

                if (world instanceof PlayingMapWorld playingWorld) {

                    // Delete the save state
                    var saveState = SaveState.optionalFromPlayer(player);
                    if (saveState != null) {
                        mapService.deleteSaveState(world.map().id(), playerData.id(), saveState.id());
                    }

                    // Remove and re-add the player without re-saving the savestate
                    playingWorld.removePlayer(player, false);
                    playingWorld.acceptPlayer(player);
                }
            }
            case HUB_CMD -> {
                try {
                    player.sendMessage("Returning to hub");

                    if (world instanceof InternalMapWorld internalWorld) {
                        internalWorld.removePlayer(player);
                    }
                    world.server().bridge().sendPlayerToHub(player);
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "failed to send player {0} to hub: {1}", player.getUuid(), e.getMessage());
                    LanguageProvider.createMultiTranslatable("command.generic.unknown_error")
                            .forEach(player::sendMessage);
                }
            }
        }
    }
}
