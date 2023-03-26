package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.context.RenderableContext;
import net.hollowcube.canvas.internal.standalone.context.RootContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class ControllerImpl implements Controller {
    private final ViewProvider viewProvider = new ViewProviderImpl();
    private final RootContext rootContext = new RootContext(viewProvider);

    private final Map<String, Object> contextObjects = new HashMap<>();

    public ControllerImpl(@NotNull Map<String, Object> context) {
        for (var entry : context.entrySet()) {
            contextObjects.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
    }

    @Override
    public void show(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        var inventory = new InventoryViewHost();
        var context = new RenderableContext(rootContext, inventory, contextObjects);
        inventory.pushView(viewProvider.apply(context));
        player.openInventory(inventory.getHandle());
    }

}
