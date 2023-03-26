package net.hollowcube.canvas.internal.standalone.context;

import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.provider.InventoryViewHost;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record RenderableContext(
        @NotNull Context parent,
        @NotNull InventoryViewHost inventory,
        @NotNull Map<String, Object> contextObjects
) implements Context, ElementContext {

    @Override
    public @NotNull ViewProvider viewProvider() {
        return parent.viewProvider();
    }

    @Override
    public void markDirty() {
        inventory.markDirty();
    }

}
