package net.hollowcube.mapmaker.gui.play.history;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.MapEntry;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapHistory;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class MapHistoryView extends View {

    private static final TextColor PAGE_COLOR = TextColor.color(0xBAC7D2);

    private @ContextObject ServerBridge bridge;
    private @ContextObject PlayerService playerService;
    private @ContextObject SessionManager sessionManager;
    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("paging") Pagination pagination;
    private @Outlet("page_number") Text pageNumber;

    public MapHistoryView(@NotNull Context context) {
        super(context);
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<MapEntry> request) {
        try {
            var history = mapService.getPlayerMapHistory(
                    this.player.getUuid().toString(),
                    request.page(),
                    request.pageSize()
            );

            var mapIds = history.results().stream().map(MapHistory.Entry::mapId).toList();

            var maps = new HashMap<String, MapData>();

            for (MapData map : mapService.getMaps(this.player.getUuid().toString(), mapIds)) {
                maps.put(map.id(), map);
            }

            var entries = new ArrayList<MapEntry>();
            for (var entry : history.results()) {
                var data = maps.get(entry.mapId());
                if (data == null) continue;

                entries.add(new MapEntry(request.context(), data));
            }

            request.respond(entries, history.nextPage());

            // Fetch the player's current progress on the maps
            if (mapIds.isEmpty()) return;
            final int page = request.page();
            async(() -> {
                var resp = mapService.getMapProgress(player.getUuid().toString(), mapIds);
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
        pageNumber.setArgs(Component.text(page + 1).color(PAGE_COLOR));
        pageNumber.setText(String.valueOf(page + 1), PAGE_COLOR);
    }
}
