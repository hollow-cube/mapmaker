package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.canvas.internal.standalone.context.TempContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ControllerImpl implements Controller {

    @Override
    public void show(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        var inventory = new InventoryViewHost(viewProvider.apply(new TempContext(new ViewProviderImpl())));
        player.openInventory(inventory.getHandle());
    }

}
