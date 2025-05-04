package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OpenStoreItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/store_menu"));
    public static final String ID = "mapmaker:store";

    private final MapServer server;

    public OpenStoreItem(@NotNull MapServer server) {
        super(ID, RIGHT_CLICK_ANY);
        this.server = server;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
    }

}
