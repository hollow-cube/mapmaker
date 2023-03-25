package net.hollowcube.map.feature.experimental.marker;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.item.ItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class MarkerItemHandler extends ItemHandler {

    public MarkerItemHandler() {
        super("mapmaker:marker", RIGHT_CLICK_BLOCK);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var pos = click.blockPosition();

        var markerId = UUID.randomUUID();
        var entity = new MarkerEntity(markerId, pos.add(0.5, 1, 0.5));
        entity.setInstance(click.instance(), entity.origin())
                .exceptionally(FutureUtil::handleException);

        click.player().sendMessage("Added");
    }

}
