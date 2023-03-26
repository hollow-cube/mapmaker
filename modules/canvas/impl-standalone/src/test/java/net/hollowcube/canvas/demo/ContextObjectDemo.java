package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ContextObjectDemo extends View {
    private @ContextObject String myContext;

    public ContextObjectDemo(@NotNull Context context) {
        super(context);
    }

    @Action("btn")
    private void handleButton(@NotNull Player player) {
        player.sendMessage(Component.text("My context is: " + myContext));
        player.closeInventory();
    }

}
