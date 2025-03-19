package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.player.PlayerService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ListMapsView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;

    private @Outlet("paging") Pagination pagination;

    private @Outlet("title") Text title;

    private final String targetId;
    private int maxPages = 0;

    public ListMapsView(@NotNull Context context, @NotNull String targetId) {
        super(context);
        this.targetId = targetId;

        if (player.getUuid().toString().equals(targetId)) {
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
            var results = mapService.searchMaps(MapSearchParams.builder(player.getUuid().toString())
                    .page(request.page())
                    .pageSize(request.pageSize())
                    .owner(this.targetId)
                    .build());

            var maps = new ArrayList<MapEntry>();
            for (var map : results.results()) {
                maps.add(new MapEntry(request.context(), map));
            }
            if (this.maxPages == 0 && request.page() == 0) {
                this.maxPages = results.pageCount();
            }
            request.respond(maps, (request.page() + 1) < this.maxPages);
        } catch (Exception e) {
            //todo feedback to user that it went wrong. Right now will load forever
            ExceptionReporter.reportException(e, player);
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
