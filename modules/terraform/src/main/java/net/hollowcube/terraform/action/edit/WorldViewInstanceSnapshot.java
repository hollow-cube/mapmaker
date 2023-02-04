package net.hollowcube.terraform.action.edit;

import net.minestom.server.instance.block.Block;
import net.minestom.server.snapshot.InstanceSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

record WorldViewInstanceSnapshot(@NotNull InstanceSnapshot snapshot) implements WorldView {

    @Override
    public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
        return snapshot.getBlock(x, y, z);
    }

}
