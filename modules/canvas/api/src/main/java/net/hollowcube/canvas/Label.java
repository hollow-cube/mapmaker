package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Label extends Element {

    void setArgs(@NotNull Component... args);

    default void setItemSprite(@NotNull ItemStack itemStack) {
        setItemSprite(itemStack, null);
    }
    void setItemSprite(@NotNull ItemStack itemStack, @Nullable Integer itemPosition);

    default void setArgs(@NotNull List<Component> args) {
        setArgs(args.toArray(new Component[0]));
    }

}
