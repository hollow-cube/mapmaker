package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Pagination2;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.gui.play.simple.*;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayMaps extends View {
    private final System.Logger logger = System.getLogger(PlayMaps.class.getSimpleName());

    private @ContextObject Query query;

    private @Outlet("paging") Pagination2 pagination;

    private @Outlet("parkour") AbstractToggle parkourToggle;
    private @Outlet("building") AbstractToggle buildingToggle;

    private @Outlet("best") AbstractToggle bestToggle;
    private @Outlet("trending") AbstractToggle trendingToggle;
    private @Outlet("approved") AbstractToggle approvedToggle;
    private @Outlet("boosted") AbstractToggle boostedToggle;
    private @Outlet("recent") AbstractToggle recentToggle;

    private boolean parkour = true;
    private boolean building = true;

    private enum SortPreset {
        APPROVED, BEST, BOOSTED, RECENT, TRENDING
    }
    private SortPreset sortPreset = SortPreset.BEST;

    private @Nullable String searchQuery = null;

    public PlayMaps(@NotNull Context context) {
        super(context);

        updateQuery();
    }

    @Signal(FilterParkourToggle.SIG_TOGGLE)
    private void handleParkourToggle(boolean selected) {
        parkour = selected;
        updateQuery();
    }

    @Signal(FilterBuildingToggle.SIG_TOGGLE)
    private void handleBuildingToggle(boolean selected) {
        building = selected;
        updateQuery();
    }

    @Signal(SortBestToggle.SIG_TOGGLE)
    private void handleBestToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.BEST;
        updateQuery();
    }

    @Signal(SortTrendingToggle.SIG_TOGGLE)
    private void handleTrendingToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.TRENDING;
        updateQuery();
    }

    @Signal(SortApprovedToggle.SIG_TOGGLE)
    private void handleApprovedToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.APPROVED;
        updateQuery();
    }

    @Signal(SortBoostedToggle.SIG_TOGGLE)
    private void handleBoostedToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.BOOSTED;
        updateQuery();
    }

    @Signal(SortRecentToggle.SIG_TOGGLE)
    private void handleRecentToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.RECENT;
        updateQuery();
    }

    private void updateQuery() {
        parkourToggle.setSelected(parkour);
        buildingToggle.setSelected(building);

        bestToggle.setSelected(sortPreset == SortPreset.BEST);
        trendingToggle.setSelected(sortPreset == SortPreset.TRENDING);
        approvedToggle.setSelected(sortPreset == SortPreset.APPROVED);
        boostedToggle.setSelected(sortPreset == SortPreset.BOOSTED);
        recentToggle.setSelected(sortPreset == SortPreset.RECENT);
    }

    // OLD STUFF

    @Action("query")
    private void changeQuery() {
        pushView(c -> new QueryMaps(c.with(Map.of("query", query))));
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination2.PageRequest<MapEntry> request) {
        try {
//            List<MapData> entries = mapStorage.queryMaps(
//                    new MapQuery("", false, true, false),
//                    request.page() * request.pageSize(), request.pageSize() + 1);
//            System.out.println("RESPONDED WITH PAGE " + entries);
//            if (query.takeQuery) {
//                query.takeQuery = false;
//                if (query.isQueryMap) {
//                    entries = mapStorage.queryMaps(
//                            new MapQuery(query.query, true, true, false),
//                            request.page() * request.pageSize(), request.pageSize() + 1);
//                } else {
//                    var json = MojangUtils.fromUsername(query.query);
//                    if (json == null) return;
//                    var uuid = UUID.fromString(
//                            json.get("id").getAsString().replaceFirst(
//                                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
//                                    "$1-$2-$3-$4-$5"));
//                    entries = mapStorage.queryMaps(
//                            new MapQuery(uuid.toString(), false, true, false),
//                            request.page() * request.pageSize(), request.pageSize() + 1);
//                }
//            } else {
//                entries = mapStorage.getLatestMaps(request.page() * request.pageSize(), request.pageSize() + 1);
//            }
//            if (entries.isEmpty()) {
//                request.respond(List.of(), false);
//                return;
//            }

//            var result = new ArrayList<MapEntry>();
//            for (int i = 0; i < Math.min(entries.size(), request.pageSize()); i++) {
//                result.add(new MapEntry(request.context(), entries.get(i)));
//            }
//            request.respond(result, entries.size() == request.pageSize() + 1);
        } catch (Exception e) {
            //todo feedback to user that it went wrong. Right now will load forever
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
