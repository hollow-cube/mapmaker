package net.hollowcube.map.feature.experimental.pathtool;

import net.hollowcube.map2.item.handler.ItemHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.feature.experimental.pathtool.PathToolFeatureHandler.thePath;

public class PathToolItemHandler extends ItemHandler {

    protected PathToolItemHandler() {
        super("mapmaker:path_tool", RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR, RIGHT_CLICK_ENTITY);
    }

    @Override
    public @NotNull Material material() {
        return Material.BLAZE_ROD;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
//        var map = MapWorld.forPlayer(player);


        if (player.isSneaking()) {
            thePath.reset();
            player.sendMessage("reset");
            return;
        }

        var newPointPos = click.blockPosition() != null ? click.placePosition() : player.getPosition();
        boolean added = thePath.addPoint(newPointPos);
        if (added) {
            player.sendMessage("added" + newPointPos);
        } else {
            player.sendMessage("not added");
        }

    }
}
