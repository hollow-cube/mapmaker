package net.hollowcube.mapmaker.hub.item;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.hub.gui.org.OrgMapsView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
        var targetOrg = USER_ORGS.get(click.player().getUuid().toString());
        if (targetOrg == null) {
            click.player().sendMessage("oopsie woopsie, you dont have an org :(");
            return;
        }
        guiController.show(click.player(), context -> new OrgMapsView(context, targetOrg));
    }

    private static final Map<String, String> USER_ORGS = Map.ofEntries(
            Map.entry("8d36737e-1c0a-4a71-87de-9906f577845e", "a52fdd3c-1a04-4b23-bc44-9cbcbd402153"), // expectational
            Map.entry("7bd5b459-1e6b-4753-8274-1fbd2fe9a4d5", "a52fdd3c-1a04-4b23-bc44-9cbcbd402153"), // emortaldev
            Map.entry("8fafc950-bd5a-4eac-ba2e-5e4b2fe0e305", "a52fdd3c-1a04-4b23-bc44-9cbcbd402153"), // lu15
            Map.entry("aceb326f-da15-45bc-bf2f-11940c21780c", "b571aed9-19f4-4032-9c06-75a4b7cf6c00") // notmattw
    );

}
