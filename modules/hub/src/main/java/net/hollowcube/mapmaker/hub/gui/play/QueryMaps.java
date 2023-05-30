package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QueryMaps extends View {
    private @Outlet("switch") Switch switchElement;
    private @ContextObject Query query;

    private Context context;

    public QueryMaps(@NotNull Context context) {
        super(context);
        this.context = context;

        MinecraftServer.getPacketListenerManager().setListener(ClientNameItemPacket.class, (packet, player) -> {
            query.query = packet.itemName();
        });
    }

    @Action("author_to_map")
    private void author_to_map(@NotNull Player player) {
        player.sendMessage("switching to map name query");
        switchElement.setOption(1);
        query.isQueryMap = true;
    }

    @Action("map_to_author")
    private void map_to_author(@NotNull Player player) {
        player.sendMessage("switching to author name query");
        switchElement.setOption(0);
        query.isQueryMap = false;
    }

    @Action("confirmation")
    private void confirm_query() {
        query.takeQuery = true;
        var new_context = context.with(Map.of("query", query));
        pushView(c -> new PlayMaps(new_context));
    }
}
