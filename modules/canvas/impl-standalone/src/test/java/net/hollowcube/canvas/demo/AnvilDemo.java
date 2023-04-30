package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnvilDemo extends View {

    private @Outlet("switch") Switch switchElement;

    public AnvilDemo(@NotNull Context context) {
        super(context);
    }

    @Action("author_to_map")
    private void author_to_map(@NotNull Player player) {
        player.sendMessage("switching to map name query");
        switchElement.setState(1);
    }

    @Action("map_to_author")
    private void map_to_author(@NotNull Player player) {
        player.sendMessage("switching to author name query");
        switchElement.setState(0);
    }

    @Action("confirmation")
    private void confirm_query(@NotNull Player player) {
        player.sendMessage("confirmed query");
        player.closeInventory();
    }
}
