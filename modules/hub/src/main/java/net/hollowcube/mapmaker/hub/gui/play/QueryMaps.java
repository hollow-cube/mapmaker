package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QueryMaps extends View {
    private @Outlet("switch") Switch switchElement;
    private @ContextObject Query query;

    private final Context context;

    public QueryMaps(@NotNull Context context) {
        super(context);
        this.context = context;

        MinecraftServer.getPacketListenerManager().setListener(ClientNameItemPacket.class, (packet, player) -> query.query = packet.itemName());
    }

    @Action("toggle_search_mode")
    private void toggleSearchMode(@NotNull Player player) {
        query.isQueryMap = !query.isQueryMap;
        player.sendMessage("IsQueryMap: " + query.isQueryMap);
        switchElement.setOption( query.isQueryMap ? 1 : 0 );
    }

    @Action("confirmation")
    private void confirm_query() {
        if (this.query.query.isBlank()) {
            popView();
        } else {
            query.takeQuery = true;
            var new_context = context.with(Map.of("query", query));
            pushView(c -> new PlayMaps(new_context));
        }
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.query.query = input;
    }
}
