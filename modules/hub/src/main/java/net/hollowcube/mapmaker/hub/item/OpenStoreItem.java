package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OpenStoreItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/store_menu"));
    public static final String ID = "mapmaker:store";

    private final Controller guiController;

    public OpenStoreItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.guiController = guiController;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        guiController.show(click.player(), StoreView::new);
    }

}
