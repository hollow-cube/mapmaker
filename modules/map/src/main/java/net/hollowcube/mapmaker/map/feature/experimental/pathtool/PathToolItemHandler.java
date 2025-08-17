package net.hollowcube.mapmaker.map.feature.experimental.pathtool;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.mapmaker.map.feature.experimental.pathtool.PathToolFeatureHandler.thePath;

public class PathToolItemHandler extends ItemHandler {

    protected PathToolItemHandler() {
        super("mapmaker:path_tool", RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR, RIGHT_CLICK_ENTITY);
    }

    @Override
    public void build(ItemStack.@NotNull Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        builder.material(Material.BLAZE_ROD);
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
