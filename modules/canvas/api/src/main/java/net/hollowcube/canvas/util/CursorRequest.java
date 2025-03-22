package net.hollowcube.canvas.util;

import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface CursorRequest {

    void respond(@NotNull ItemStack stack);
}
