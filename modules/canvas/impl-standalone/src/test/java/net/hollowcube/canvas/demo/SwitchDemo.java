package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SwitchDemo extends View {

    private @Outlet("switch") Switch switchElement;

    public SwitchDemo(@NotNull Context context) {
        super(context);
    }

    @Action("red")
    private void red(@NotNull Player player) {
        player.sendMessage("red");
        switchElement.setOption(1);
    }

    @Action("green")
    private void green(@NotNull Player player) {
        player.sendMessage("green");
        switchElement.setOption(2);
    }

    @Action("blue")
    private void blue(@NotNull Player player) {
        player.sendMessage("blue");
        switchElement.setOption(0);
    }

}
