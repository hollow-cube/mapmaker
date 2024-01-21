package net.hollowcube.canvas.internal.standalone.context;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.provider.InventoryViewHost;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record RenderableContext(
        @NotNull Context parent,
        @NotNull InventoryViewHost inventory,
        @NotNull Map<String, Object> contextObjects
) implements Context, ElementContext {

    @Override
    public @NotNull Context with(@NotNull Map<String, Object> contextObjects) {
        var newContextObjects = new HashMap<>(this.contextObjects);
        newContextObjects.putAll(contextObjects);
        return new RenderableContext(parent, inventory, newContextObjects);
    }

    @Override
    public @NotNull ViewProvider viewProvider() {
        return parent.viewProvider();
    }

    @Override
    public @NotNull Player player() {
        return contextObjects.get("player") instanceof Player player ? player : inventory.player();
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        inventory.performSignal(name.toLowerCase(Locale.ROOT), args);
    }

    @Override
    public void markDirty() {
        inventory.markDirty();
    }

    @Override
    public boolean canPopView() {
        return inventory.canPopView();
    }

    @Override
    public void pushView(@NotNull View view, boolean isTransient) {
        inventory.pushView(view, isTransient);
    }

    @Override
    public void popView() {
        inventory.popView();
    }
}
