package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapSearchRequest;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ListMapsView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;

    private @Outlet("paging") Pagination pagination;

    private @Outlet("title") Text title;

    private final MapPlayerData target;

    public ListMapsView(@NotNull Context context, @NotNull MapPlayerData target) {
        super(context);
        this.target = target;

        if (player.getUuid().toString().equals(target.id())) {
            title.setText("Your Maps");
        } else {
            title.setText("todo's Maps");
        }

        updateQuery(false);
    }

    private void updateQuery(boolean refresh) {
        //todo

        if (refresh) pagination.reset();
    }

    // Pagination view

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<MapEntry> request) {
        try {
            var queryResult = mapService.searchMaps(MapSearchRequest.builder(player.getUuid().toString())
                    .page(request.page(), request.pageSize())
                    .owner(target.id())
                    .build());

            var maps = new ArrayList<MapEntry>();
            for (var map : queryResult.results()) {
                maps.add(new MapEntry(request.context(), map));
            }
            request.respond(maps, queryResult.nextPage());
        } catch (Exception e) {
            //todo feedback to user that it went wrong. Right now will load forever
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Action("next_page")
    public void nextPage() {
        pagination.nextPage();
    }

    @Action("prev_page")
    public void prevPage() {
        pagination.prevPage();
    }

}
