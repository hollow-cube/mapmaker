package net.hollowcube.mapmaker.hub.item;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.hub.gui.org.OrgMapsView;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class OrgMapsItem extends ItemHandler {
    public static final String ID = "mapmaker:org_maps";

    private final Controller guiController;

    @Inject
    public OrgMapsItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.guiController = guiController;
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND_PICKAXE;
    }

    @Override
    public int customModelData() {
        return 5556;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        guiController.show(click.player(), context -> new OrgMapsView(context, "b571aed9-19f4-4032-9c06-75a4b7cf6c00"));
    }

}
