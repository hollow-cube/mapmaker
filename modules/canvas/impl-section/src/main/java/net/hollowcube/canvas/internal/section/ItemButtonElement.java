package net.hollowcube.canvas.internal.section;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.section.ClickHandler;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ItemButtonElement extends ItemLabelElement {
    private final List<ClickHandler> handlers = new ArrayList<>();

    public ItemButtonElement(@Nullable String id, int width, int height,
                             @NotNull String translationKey, @NotNull Component... args) {
        super(id, width, height, translationKey, args);
    }

    public ItemButtonElement(@NotNull ItemButtonElement other) {
        super(other);
    }

    @Override
    public void wireAction(@NotNull View owner, @NotNull Method method) {
        method.setAccessible(true);
        handlers.add((player, slot, clickType) -> {
            try {
                var args = new ArrayList<>();
                if (method.getParameterCount() > 0)
                    args.add(player);
                method.invoke(owner, args.toArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke action method " + method, e);
            }
            return ClickHandler.DENY;
        });
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        for (ClickHandler handler : handlers) {
            handler.handleClick(player, slot, clickType);
        }
        return ClickHandler.DENY;
    }

    @Override
    public BaseElement clone() {
        return new ItemButtonElement(this);
    }
}
