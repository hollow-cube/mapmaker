package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapDetailsItem extends ItemHandler {

    public static final String ID = "mapmaker:map_details";
    public static final MapDetailsItem INSTANCE = new MapDetailsItem();

    private MapDetailsItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.MAP;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        player.sendMessage("TODO: show map details GUI");
    }

}
