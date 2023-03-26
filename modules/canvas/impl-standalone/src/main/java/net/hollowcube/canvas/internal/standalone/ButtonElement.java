package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ButtonElement extends LabelElement {
    @FunctionalInterface
    public interface ClickHandler {
        boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType);
    }

    private final List<ClickHandler> handlers = new ArrayList<>();

    public ButtonElement(@NotNull ElementContext context, @Nullable String id, int width, int height,
                         @NotNull String translationKey) {
        super(context, id, width, height, translationKey);
    }

    protected ButtonElement(@NotNull ElementContext context, @NotNull ButtonElement other) {
        super(context, other);
    }

    @Override
    public boolean handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput()) return CLICK_DENY;

        for (var handler : handlers) {
            handler.handleClick(player, slot, clickType);
        }
        return CLICK_DENY;
    }

    @Override
    public void wireAction(@NotNull View owner, @NotNull Method method) {
        method.setAccessible(true); // NOSONAR
        handlers.add((player, slot, clickType) -> {
            if (method.getParameterCount() < 3 && clickType != ClickType.LEFT_CLICK)
                return CLICK_DENY;
            try {
                var args = new ArrayList<>();
                if (method.getParameterCount() > 0)
                    args.add(player);
                if (method.getParameterCount() > 1)
                    args.add(slot);
                if (method.getParameterCount() > 2)
                    args.add(clickType);
                method.invoke(owner, args.toArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke action method " + method, e);
            }
            return CLICK_DENY;
        });
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new ButtonElement(context, this);
    }
}
