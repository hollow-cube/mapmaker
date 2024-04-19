package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ButtonElement extends LabelElement {
    @FunctionalInterface
    public interface ClickHandler {
        @Nullable
        Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType);
    }

    private ClickHandler handler;

//    private final List<ClickHandler> handlers = new ArrayList<>();

    public ButtonElement(@NotNull ElementContext context, @Nullable String id, int width, int height,
                         @NotNull String translationKey) {
        super(context, id, width, height, translationKey);
    }

    protected ButtonElement(@NotNull ElementContext context, @NotNull ButtonElement other) {
        super(context, other);
    }

    @Override
    public @Nullable Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput() || handler == null) return null;
        return handler.handleClick(player, slot, clickType);
    }

    @Override
    public void wireAction(@NotNull View owner, @NotNull Object handler, @NotNull Action.Descriptor action) {
        if (this.handler != null)
            throw new IllegalStateException("Cannot wire multiple action handlers to a button");
        switch (handler) {
            case Method method -> {
                method.setAccessible(true); // NOSONAR
                this.handler = (player, slot, clickType) -> {
                    try {
                        if (method.getParameterCount() < 3 && clickType != ClickType.LEFT_CLICK)
                            return null;
                        Callable<@Nullable Future<Void>> doCall = () -> {
                            try {
                                var args = new ArrayList<>();
                                if (method.getParameterCount() > 0)
                                    args.add(player);
                                if (method.getParameterCount() > 1)
                                    args.add(slot);
                                if (method.getParameterCount() > 2)
                                    args.add(clickType);
                                var result = method.invoke(owner, args.toArray());
                                if (result instanceof Future)
                                    return (Future<Void>) result;
                                return null;
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to invoke action method " + method, e);
                            }
                        };
                        if (action.async()) {
                            return VIRTUAL_EXECUTOR.submit(() -> {
                                try {
                                    var future = doCall.call();
                                    if (future != null)
                                        future.get();
                                    return null;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } else {
                            return doCall.call();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            case Label.ActionHandler actionHandler -> {
                this.handler = (player, slot, clickType) -> {
                    actionHandler.handle(player, slot, clickType);
                    return null;
                };
            }
            default -> throw new UnsupportedOperationException("Unsupported action handler: " + handler);
        }
    }

    @Override
    public @NotNull LabelElement clone(@NotNull ElementContext context) {
        return new ButtonElement(context, this);
    }
}
