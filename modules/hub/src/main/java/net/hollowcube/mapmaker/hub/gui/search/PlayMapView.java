package net.hollowcube.mapmaker.hub.gui.search;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.hub.gui.common.TranslatedButtonSection;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PlayMapView extends ParentSection {
    private static final Logger logger = LoggerFactory.getLogger(PlayMapView.class);

    private final MapData map;

    public PlayMapView(@NotNull MapData map) {
        super(9, 6);
        this.map = map;

        // Header
        add(0, 0, new BackOrCloseButton());
        add(0, 4, new MapEntryButton(map, false));

        // Content buttons
        add(4, 2, new TranslatedButtonSection(1, 1, "gui.play_map.play", List.of(), Material.EMERALD_BLOCK, this::playMap));

        // Footer
        add(8, 5, new ButtonSection(1, 1, ItemStack.of(Material.ANVIL)
                .withDisplayName(Component.text("Report map (todo)"))));
    }

    private boolean playMap(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType != ClickType.LEFT_CLICK) return ClickHandler.DENY;

        player.closeInventory();
        getContext(Handler.class).playMap(player, map.getId())
                .thenErr(err -> {
                    // If an error occurs here the player is still here, it is our responsibility to handle this (with an error)
                    logger.error("failed to join map {} for {}: {}", map.getId(), PlayerData.fromPlayer(player).getId(), err.message());
                    player.sendMessage(Component.translatable("command.generic.unknown_error"));
                });
        return ClickHandler.DENY;
    }

}
