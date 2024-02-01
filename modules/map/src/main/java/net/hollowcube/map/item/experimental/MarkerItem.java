package net.hollowcube.map.item.experimental;

import net.hollowcube.map.item.handler.ItemHandler;
import org.jetbrains.annotations.NotNull;

public class MarkerItem extends ItemHandler {
    public static final MarkerItem INSTANCE = new MarkerItem();

    private MarkerItem() {
        super("mapmaker:marker", RIGHT_CLICK_BLOCK);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var pos = click.blockPosition();

        click.player().sendMessage("todo need to add at " + pos.add(0.5, 1, 0.5));
    }

}
