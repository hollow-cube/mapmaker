package net.hollowcube.canvas;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickHandler {
    boolean ALLOW = true;
    boolean DENY = false;

    boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType);

    static @NotNull ClickHandler leftClick(@NotNull Runnable runnable) {
        return (player, slot, clickType) -> {
            if (clickType == ClickType.LEFT_CLICK)
                runnable.run();
            return DENY;
        };
    }
}
