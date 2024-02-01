package net.hollowcube.terraform.task.edit;

import net.hollowcube.terraform.task.Task;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

record WorldViewEmpty(@NotNull Task task) implements WorldView {

    @Override
    public boolean contains(int x, int y, int z) {
        return false;
    }

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Block.Getter.Condition condition) {
        return Block.AIR;
    }
}
