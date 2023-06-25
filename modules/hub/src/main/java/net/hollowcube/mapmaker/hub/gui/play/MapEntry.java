package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class MapEntry extends View {

    private @ContextObject PlayerService playerService;

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
    private void handleClick() {
        pushView(c -> new MapDetailsView(c, map, authorName));
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
                map.getCompletionStateText()
        );

        label.setState(State.ACTIVE);
    }

}
