package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination2;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.minestom.server.utils.mojang.MojangUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayMaps extends View {
    private final System.Logger logger = System.getLogger(PlayMaps.class.getSimpleName());

    private @ContextObject Query query;

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
                List<MapData> entries = Collections.emptyList();
                if (query.takeQuery) {
                    query.takeQuery = false;
                    if (query.isQueryMap) {
                        entries = mapStorage.queryMaps(
                                new MapQuery(query.query, true, true, false),
                                request.page() * request.pageSize(), request.pageSize() + 1);
                    } else {
                        var json = MojangUtils.fromUsername(query.query);
                        if (json == null) return;
                        var uuid = UUID.fromString(
                                json.get("id").getAsString().replaceFirst(
                                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                                        "$1-$2-$3-$4-$5"));
                        entries = mapStorage.queryMaps(
                                new MapQuery(uuid.toString(), false, true, false),
                                request.page() * request.pageSize(), request.pageSize() + 1);
                    }
                }
                else {
                    entries = mapStorage.getLatestMaps(request.page() * request.pageSize(), request.pageSize() + 1);
                }
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
        pushView(c -> new QueryMaps(c.with(Map.of("query", query))));
    }
}
