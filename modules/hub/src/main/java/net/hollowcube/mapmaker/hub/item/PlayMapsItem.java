package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.gui.map.browser.MapBrowserView;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("tablet"), "tablet");
    public static final String ID = "mapmaker:play_maps";

    private final PlayerService playerService;
    private final MapService mapService;

    public PlayMapsItem(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super(ID, RIGHT_CLICK_ANY);
        this.playerService = playerService;
        this.mapService = mapService;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        Panel.open(player, new MapBrowserView(playerService, mapService));
    }

}
