package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class MapEntry extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject HubHandler handler;

    private @Outlet("btn") Label label;

    private final PersonalizedMapData map;
    private Component authorName;

    public MapEntry(@NotNull Context context, @NotNull PersonalizedMapData map) {
        super(context);
        this.map = map;

        label.setState(State.LOADING);
        async(this::updateIcon);
    }

    @Action("btn")
    private void handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case START_SHIFT_CLICK -> handler.playMap(player, map.id());
            case LEFT_CLICK -> pushView(c -> new MapDetailsView(c, map, authorName));
        }
    }

    /** Builds and updates the arg list of the map icon. */
    private @Blocking void updateIcon() {

        var icon = map.settings().getIcon();
        label.setItemSprite(ItemStack.of(icon == null ? Material.PAPER : icon));

        authorName = playerService.getPlayerDisplayName(map.owner());
        label.setArgs(
                Component.text(map.publishedIdString()),
                map.settings().getNameComponent(),
                authorName,
                map.getCompletionStateText(),
                getDifficulty()
        );

        label.setState(State.ACTIVE);
    }

    private @NotNull Component getDifficulty() {
        if (map.getUniquePlays() < PersonalizedMapData.MIN_PLAYS_FOR_DIFFICULTY)
            return Component.translatable("gui.play_maps.map_display.difficulty.unknown");

        return Component.translatable(
                "gui.play_maps.map_display.difficulty." + getDifficultyName(),
                Component.text(getClearRateString())
        );
    }

    private @NotNull String getDifficultyName() {
        var cr = map.getClearRate();
        if (cr < 0.015) return "nightmare";
        if (cr < 0.075) return "expert";
        if (cr < 0.2) return "hard";
        if (cr < 0.4) return "medium";
        return "easy";
    }

    private @NotNull String getClearRateString() {
        var cr = map.getClearRate() * 100;
        if (cr >= 100) return "100";
        else if (cr <= 0) return "0";
        else if (cr >= 10) return String.format("%.1f", cr);
        else if (cr >= 1) return String.format("%.2f", cr);
        else return String.format("%.3f", cr);
    }

}
