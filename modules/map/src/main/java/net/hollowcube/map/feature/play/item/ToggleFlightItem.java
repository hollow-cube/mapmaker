package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ToggleFlightItem extends ItemHandler {

    public static final String ID = "mapmaker:toggle_flight";
    public static final ToggleFlightItem INSTANCE = new ToggleFlightItem();

    private ToggleFlightItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.FEATHER;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        if (player.isAllowFlying()) {
            player.setFlying(false);
            player.setAllowFlying(false);
        } else {
            player.setFlying(true);
            player.setAllowFlying(true);
        }
    }
}
