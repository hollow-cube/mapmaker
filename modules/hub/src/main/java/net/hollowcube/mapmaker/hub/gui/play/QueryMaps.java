package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import org.jetbrains.annotations.NotNull;

public class QueryMaps extends View {
    private @Outlet("switch") Switch switchElement;

    private Context context;
    private String query = null;
    private boolean isQueryMap;

    public QueryMaps(@NotNull Context context) {
        super(context);
        this.context = context;

        MinecraftServer.getPacketListenerManager().setListener(ClientNameItemPacket.class, (packet, player) -> {
            this.query = packet.itemName();
        });
    }

    @Action("author_to_map")
    private void author_to_map(@NotNull Player player) {
        player.sendMessage("switching to map name query");
        switchElement.setState(1);
        this.isQueryMap = true;
    }

    @Action("map_to_author")
    private void map_to_author(@NotNull Player player) {
        player.sendMessage("switching to author name query");
        switchElement.setState(0);
        this.isQueryMap = false;
    }

    @Action("confirmation")
    private void confirm_query() {
        context.performSignal("query", this.query, this.isQueryMap);
        popView();
    }
}
