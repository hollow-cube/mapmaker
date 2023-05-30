package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import org.jetbrains.annotations.NotNull;

public class AnvilDemo extends View {

    private @Outlet("switch") Switch switchElement;
    private String search;

    public AnvilDemo(@NotNull Context context) {
        super(context);
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    private void anvilInput(@NotNull String input) {
        search = input;
    }

    @Action("author_to_map")
    private void authorToMap(@NotNull Player player) {
        player.sendMessage("switching to map name query");
        switchElement.setOption(1);
    }

    @Action("map_to_author")
    private void mapToAuthor(@NotNull Player player) {
        player.sendMessage("switching to author name query");
        switchElement.setOption(0);
    }

    @Action("confirmation")
    private void confirmQuery(@NotNull Player player) {
        player.sendMessage("confirmed query " + search);
        player.closeInventory();
    }
}
