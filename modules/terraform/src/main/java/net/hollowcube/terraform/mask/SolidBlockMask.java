package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class SolidBlockMask implements Mask {

    @Override
    public boolean test(@NotNull WorldView world, @NotNull Point point, @NotNull Block block) {
        return block.isSolid();
    }
}
