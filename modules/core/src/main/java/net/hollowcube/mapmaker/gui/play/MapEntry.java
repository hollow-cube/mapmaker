package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class MapEntry extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject ServerBridge bridge;

    private @Outlet("btn") Label label;

    private final MapData map;
    private PersonalizedMapData.Progress progress = null; // null is unknown
    private int playtime = 0;
    private DisplayName authorName = null;

    public MapEntry(@NotNull Context context, @NotNull MapData map) {
        super(context);
        this.map = map;

        label.setState(State.LOADING);
        async(this::updateIcon);
    }

    public @NotNull MapData map() {
        return map;
    }

    public void setProgress(PersonalizedMapData.Progress progress, int playtime) {
        this.progress = progress;
        this.playtime = playtime;
        async(this::updateIcon);
    }

    @Action("btn")
    private void handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case SHIFT_LEFT_CLICK ->
                    bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.PLAYING, "play_maps_gui");
            case LEFT_CLICK -> pushView(c -> new MapDetailsView(c, map, authorName));
        }
    }

    /**
     * Builds and updates the arg list of the map icon.
     */
    private @Blocking void updateIcon() {
        var icon = map.settings().getIcon();
        if (icon == null) {
            label.setItemSprite(ItemStack.of(Material.PAPER));
        } else {
            var item = ItemStack.builder(icon);
            TagUtil.removeTooltipExtras(item);
            label.setItemSprite(item.build());
        }

        // todo we could update the icon + title immediately and only update the lore once we have the player name perhaps
        if (authorName == null) {
            try {
                authorName = playerService.getPlayerDisplayName2(map.owner());
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                authorName = new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
            }
        }

        var entry = MapData.createHoverComponents(map, authorName.build(),
                progress == null ? null : Map.entry(progress, playtime));
        label.setComponentsDirect(entry.getKey(), entry.getValue());

        label.setState(State.ACTIVE);
    }
}
