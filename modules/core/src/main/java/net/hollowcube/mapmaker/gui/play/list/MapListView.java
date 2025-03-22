package net.hollowcube.mapmaker.gui.play.list;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.MapEntry;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MapListView extends View {

    private static final TextColor PAGE_COLOR = TextColor.color(0xBAC7D2);

    private @ContextObject PlayerService playerService;
    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("paging") Pagination pagination;
    private @Outlet("page_number") Text pageNumber;
    private @Outlet("title") Text title;

    private final String targetId;
    private int maxPages = 0;

    public MapListView(@NotNull Context context, @NotNull String targetId) {
        super(context);

        this.targetId = targetId;

        if (player.getUuid().toString().equals(targetId)) {
            title.setText("Your Maps");
        } else {
            title.setText(playerService.getPlayerDisplayName2(targetId).getUsername() + "'s Maps");
        }
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<MapEntry> request) {
        try {
            var response = mapService.searchMaps(MapSearchParams.builder(player.getUuid().toString())
                    .page(request.page())
                    .pageSize(request.pageSize())
                    .owner(this.targetId)
                    .build());

            var entries = new ArrayList<MapEntry>();
            var ids = new ArrayList<String>();
            for (var map : response.results()) {
                entries.add(new MapEntry(request.context(), map));
                ids.add(map.id());
            }
            if (request.page() == 0) {
                this.maxPages = response.pageCount();
            }

            request.respond(entries, (request.page() + 1) < this.maxPages);

            // Fetch the player's current progress on the maps
            if (ids.isEmpty()) return;
            final int page = request.page();
            async(() -> {
                var resp = mapService.getMapProgress(player.getUuid().toString(), ids);
                player.scheduleNextTick(ignored -> {
                    if (page != pagination.page()) return;
                    pagination.<MapEntry>forEachEntry(page, entry -> {
                        var progress = resp.getProgress(entry.map().id());
                        if (progress != null) entry.setProgress(progress.getKey(), progress.getValue());
                    });
                });
            });

            updatePageNumber(page);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, this.player);
        }
    }

    @Action("back")
    private void back() {
        pagination.prevPage();
        updatePageNumber(pagination.page());
    }

    @Action("next")
    private void next() {
        pagination.nextPage();
        updatePageNumber(pagination.page());
    }

    private void updatePageNumber(int page) {
        pageNumber.setArgs(Component.text(String.format("%d/%d", page + 1, maxPages)).color(PAGE_COLOR));
        pageNumber.setText(String.format("%d/%d", page + 1, maxPages), PAGE_COLOR);
    }
}
