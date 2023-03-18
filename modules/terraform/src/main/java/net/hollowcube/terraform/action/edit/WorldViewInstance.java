package net.hollowcube.terraform.action.edit;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record WorldViewInstance(@NotNull Instance instance) implements WorldView {
    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return instance.getBlock(x, y, z);
    }
}
