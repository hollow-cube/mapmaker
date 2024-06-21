package net.hollowcube.mapmaker.hub.item;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenStoreItem extends ItemHandler {
    public static final String ID = "mapmaker:store";

    private final Controller guiController;

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/store_menu"));

    @Inject
    public OpenStoreItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.guiController = guiController;
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        guiController.show(click.player(), StoreView::new);
    }

}
