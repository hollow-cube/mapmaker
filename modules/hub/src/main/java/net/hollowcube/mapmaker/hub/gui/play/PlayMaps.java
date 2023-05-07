package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination2;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.storage.MapStorage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayMaps extends View {
    private final System.Logger logger = System.getLogger(PlayMaps.class.getSimpleName());

    private String query = null;
    private boolean isQueryMap;

    private @ContextObject MapStorage mapStorage;

    private @Outlet("paging") Pagination2 pagination;
    private @Outlet("parkour_toggle") Label parkourToggle;

    public PlayMaps(@NotNull Context context) {
        super(context);
    }

    @Action("paging")
    private void fetchPage(@NotNull Pagination2.PageRequest<MapEntry> request) {
        //todo could support async in this action
        Thread.startVirtualThread(() -> {
            try {
                if (query != null) {
                    logger.log(System.Logger.Level.WARNING, "fetch page with %s %b".formatted(query, isQueryMap));
                }
                var entries = mapStorage.getLatestMaps(request.page() * request.pageSize(), request.pageSize() + 1);
                if (entries.isEmpty()) {
                    request.respond(List.of(), false);
                    return;
                }

                var result = new ArrayList<MapEntry>();
                for (int i = 0; i < Math.min(entries.size(), request.pageSize()); i++) {
                    result.add(new MapEntry(request.context(), entries.get(i)));
                }
                request.respond(result, entries.size() == request.pageSize() + 1);
            } catch (Exception e) {
                //todo log in sentry
                //todo feedback to user that it went wrong. Right now will load forever
                logger.log(System.Logger.Level.ERROR, "Failed to fetch page", e);
            }
        });
    }

    @Action("parkour_toggle")
    private void parkourToggle() {
        parkourToggle.setState(parkourToggle.getState() == State.ACTIVE ? State.HIDDEN : State.ACTIVE);
    }

    @Action("query")
    private void changeQuery() {
        pushView(QueryMaps::new);
        logger.log(System.Logger.Level.WARNING, "changeQuery called");
    }

    @Signal("query")
    private void receiveQuery(@NotNull String query, @NotNull boolean isQueryMap) {
        this.query = query;
        this.isQueryMap = isQueryMap;
        logger.log(System.Logger.Level.WARNING, "received query with %s".formatted(query));
    }
}
