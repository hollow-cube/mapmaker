package net.hollowcube.map.gui.hotbar;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.feature.checkpoint.CheckpointCache;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
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

    private static final int MAP_DETAILS_CMD = 505;
    private static final int RETURN_TO_CHECKPOINT_CMD = 506;
    private static final int START_SPECTATE_CMD = 507;
    private static final int RESET_CMD = 508;
    private static final int HUB_CMD = 509;
    private static final ItemStack MAP_DETAILS_ITEM = ItemStack.builder(Material.MAP)
            .displayName(Component.translatable("hotbar.map_details.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.map_details.lore", List.of()))
            .meta(meta -> meta.customModelData(MAP_DETAILS_CMD))
            .build();
    private static final ItemStack RETURN_TO_CHECKPOINT_ITEM = ItemStack.builder(Material.RED_DYE)
            .displayName(Component.translatable("hotbar.return_to_checkpoint.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.return_to_checkpoint.lore", List.of()))
            .meta(meta -> meta.customModelData(RETURN_TO_CHECKPOINT_CMD))
            .build();
    private static final ItemStack START_SPECTATE_ITEM = ItemStack.builder(Material.CLAY_BALL)
            .displayName(Component.translatable("hotbar.start_spectating.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.start_spectating.lore", List.of()))
            .meta(meta -> meta.customModelData(START_SPECTATE_CMD))
            .build();
    private static final ItemStack RESET_ITEM = ItemStack.builder(Material.REDSTONE)
            .displayName(Component.translatable("hotbar.regular_play.reset.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.regular_play.reset.lore", List.of()))
            .meta(meta -> meta.customModelData(RESET_CMD))
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
        if (!CheckpointCache.forInstance(world.instance()).isEmpty())
            // Only add if there are checkpoints present in the map
            player.getInventory().setItemStack(1, RETURN_TO_CHECKPOINT_ITEM);
        player.getInventory().setItemStack(4, START_SPECTATE_ITEM);
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
            case START_SPECTATE_CMD -> {
                ((InternalMapWorld) world).removePlayer(player);
                if (world instanceof PlayingMapWorld playingWorld) {
                    playingWorld.startSpectating(player, false);
                }
            }
            case RESET_CMD -> {
                //todo add this in a common place and a /reset command
                var mapService = world.server().mapService();
                var playerData = PlayerDataV2.fromPlayer(player);

                if (world instanceof PlayingMapWorld playingWorld) {

                    // Delete the save state
                    var saveState = SaveState.optionalFromPlayer(player);
                    if (saveState != null) {
                        player.removeTag(MapHooks.PLAYING);
                        saveState.setPlaytime(0);
                        saveState.setPlayStartTime(System.currentTimeMillis());
                        saveState.setCompleted(false);
                        player.teleport(world.map().settings().getSpawnPoint()).join();
                        //todo this will not clear effects or anything, i guess the plate fp will have to do that
                        player.setTag(MapHooks.PLAYING, true);
                        EventDispatcher.call(new MapPlayerInitEvent(world, player, false));
                    } else {
                        // The player has no save state because they are spectating, so just re-add them to the server
                        playingWorld.removePlayer(player, false);
                        playingWorld.acceptPlayer(player, false);
                    }

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
                    LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                            .forEach(player::sendMessage);
                }
            }
        }
    }
}
