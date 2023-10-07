package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface Label extends Element {

    void setArgs(@NotNull Component... args);

    default void setItemSprite(@NotNull ItemStack itemStack) {
        setItemSprite(itemStack, null);
    }

    void setItemSprite(@NotNull ItemStack itemStack, @Nullable Integer itemPosition);

    default void setArgs(@NotNull List<Component> args) {
        setArgs(args.toArray(new Component[0]));
    }

    interface ActionHandler {
        static @NotNull ActionHandler lmb(@NotNull Consumer<Player> handler) {
            return (player, slot, clickType) -> {
                if (clickType != ClickType.LEFT_CLICK) return;
                handler.accept(player);
            };
        }

        void handle(@NotNull Player player, int slot, @NotNull ClickType clickType);
    }

}
