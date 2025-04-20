package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
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

    void setSprite(char fontChar, @Nullable String model, int width, int offsetX, int rightOffset);

    default void setArgs(@NotNull List<Component> args) {
        setArgs(args.toArray(new Component[0]));
    }

    /**
     * This is a bad function which will make porting to a proxy based solution difficult,
     * however I do not have an immediately easier way to do it.
     *
     * <p>When this is done on a proxy in Go, I would use text/template to render conditional parts.</p>
     *
     * @param title The item title, if present
     * @param lore  The item lore, if present
     */
    @Deprecated
    void setComponentsDirect(@Nullable Component title, @Nullable List<Component> lore);

    @Deprecated
    void setItemDirect(@NotNull ItemStack itemStack);

    @NotNull ItemStack getItemDirect();

    void setSpriteColorModifier(@NotNull TextColor color);

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
