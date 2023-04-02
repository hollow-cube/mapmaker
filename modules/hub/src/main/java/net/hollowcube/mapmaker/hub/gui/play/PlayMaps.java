package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination2;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.storage.MapStorage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayMaps extends View {

    private @ContextObject MapStorage mapStorage;

    private @Outlet("paging") Pagination2 pagination;
    private @Outlet("parkour_toggle") Label parkourToggle;

    public PlayMaps(@NotNull Context context) {
        super(context);
    }

    @Action("paging")
    private void fetchPage(@NotNull Pagination2.PageRequest<MapEntry> request) {
        mapStorage.getLatestMaps(request.page() * request.pageSize(), request.pageSize() + 1)
                .then(entries -> {
                    if (entries.isEmpty()) {
                        request.respond(List.of(), false);
                        return;
                    }

                    var result = new ArrayList<MapEntry>();
                    for (int i = 0; i < Math.min(entries.size(), request.pageSize()); i++) {
                        result.add(new MapEntry(request.context()));
                    }
                    request.respond(result, entries.size() == request.pageSize() + 1);
                })
                .thenErr(err -> {
                    throw new RuntimeException(err.message());
                });
    }

    @Action("parkour_toggle")
    private void parkourToggle() {
        parkourToggle.setState(parkourToggle.getState() == State.ACTIVE ? State.HIDDEN : State.ACTIVE);
    }
}
