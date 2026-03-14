package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.map.browser.MapBrowserView;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PlayMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("tablet"), "tablet");
    public static final Key ID = Key.key("mapmaker:play_maps");

    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;

    public PlayMapsItem(ApiClient api, MapService mapService, ServerBridge bridge) {
        super(ID, RIGHT_CLICK_ANY);
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        Panel.open(player, new MapBrowserView(api, mapService, bridge));
    }

}
