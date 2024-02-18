package net.hollowcube.mapmaker.hub.item;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.gui.store.CosmeticView;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class OpenCosmeticsMenuItem extends ItemHandler {
    public static final String ID = "mapmaker:cosmetics";

    private final Controller guiController;

    @Inject
    public OpenCosmeticsMenuItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.guiController = guiController;
    }

    @Override
    public @NotNull Material material() {
        return Material.ARMOR_STAND;
    }

    @Override
    public int customModelData() {
        return 5555;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        guiController.show(click.player(), CosmeticView::new);
    }

}
