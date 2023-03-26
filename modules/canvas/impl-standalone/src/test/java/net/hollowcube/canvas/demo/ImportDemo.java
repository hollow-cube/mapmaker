package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ImportDemo extends View {

    // This shadows a name within SwitchDemo, so we should reference it correctly here,
    // and it should still work within SwitchDemo
    private @Outlet("switch") SwitchDemo switchDemo;

    public ImportDemo(@NotNull Context context) {
        super(context);
    }

    @Action("btn")
    private void handleButton(@NotNull Player player) {
        player.sendMessage("Button pressed, switch demo is " + switchDemo);
    }

}
