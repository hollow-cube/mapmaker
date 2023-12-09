package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.simple.*;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PlayMapsView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject Player player;
    private @ContextObject String query;

    private @Outlet("page_text") Text pageText;
    private @Outlet("paging") Pagination pagination;

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

    private SortPreset sortPreset = SortPreset.RECENT;


    public PlayMapsView(@NotNull Context context) {
        super(context);
        updateQuery(false);
    }

    // Map type filter

    @Signal(FilterParkourToggle.SIG_TOGGLE)
    private void handleParkourToggle(boolean selected) {
        parkour = selected;
        updateQuery(true);
    }

    @Signal(FilterBuildingToggle.SIG_TOGGLE)
    private void handleBuildingToggle(boolean selected) {
        building = selected;
        updateQuery(true);
    }

    // Simple preset

    @Signal(SortBestToggle.SIG_TOGGLE)
    private void handleBestToggle(boolean selected) {
        // INTENTIONALLY DISABLED UNTIL IMPLEMENTED PROPERLY
//        if (!selected) return;
//
//        sortPreset = SortPreset.BEST;
//        updateQuery();
    }

    @Signal(SortTrendingToggle.SIG_TOGGLE)
    private void handleTrendingToggle(boolean selected) {
        // INTENTIONALLY DISABLED UNTIL IMPLEMENTED PROPERLY
//        if (!selected) return;
//
//        sortPreset = SortPreset.TRENDING;
//        updateQuery();
    }

    @Signal(SortApprovedToggle.SIG_TOGGLE)
    private void handleApprovedToggle(boolean selected) {
        // INTENTIONALLY DISABLED UNTIL IMPLEMENTED PROPERLY
//        if (!selected) return;
//
//        sortPreset = SortPreset.APPROVED;
//        updateQuery();
    }

    @Signal(SortBoostedToggle.SIG_TOGGLE)
    private void handleBoostedToggle(boolean selected) {
        // INTENTIONALLY DISABLED UNTIL IMPLEMENTED PROPERLY
//        if (!selected) return;
//
//        sortPreset = SortPreset.BOOSTED;
//        updateQuery();
    }

    @Signal(SortRecentToggle.SIG_TOGGLE)
    private void handleRecentToggle(boolean selected) {
        if (!selected) return;

        sortPreset = SortPreset.RECENT;
        updateQuery(true);
    }

    private void updateQuery(boolean refresh) {
        parkourToggle.setSelected(parkour);
        buildingToggle.setSelected(building);

        bestToggle.setSelected(sortPreset == SortPreset.BEST);
        trendingToggle.setSelected(sortPreset == SortPreset.TRENDING);
        approvedToggle.setSelected(sortPreset == SortPreset.APPROVED);
        boostedToggle.setSelected(sortPreset == SortPreset.BOOSTED);
        recentToggle.setSelected(sortPreset == SortPreset.RECENT);

        if (refresh) pagination.reset();
    }

    // Pagination view

    private int currentPage = 0;
    private int maxPages = 0;

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<MapEntry> request) {
        try {
            var queryResult = mapService.searchMaps(player.getUuid().toString(), request.page(), request.pageSize(), building, parkour, query);

            var maps = new ArrayList<MapEntry>();
            for (var map : queryResult.results()) {
                maps.add(new MapEntry(request.context(), map));
            }
            request.respond(maps, queryResult.nextPage());

            maxPages = request.page() + 1;
            currentPage = request.page() + 1;
            updatePageText();
        } catch (Exception e) {
            player.closeInventory();
            player.sendMessage(Component.translatable("generic.unknown_error"));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Action("next_page")
    public void nextPage() {
        if (currentPage < maxPages) { // Check if it's not the last page
            currentPage++;
            updatePageText();
        }
        pagination.nextPage();
    }

    @Action("prev_page")
    public void prevPage() {
        if (currentPage > 1) { // Check if it's not the first page
            currentPage--;
            updatePageText();
        }
        pagination.prevPage();
    }

    @Action("page_text")
    public void resetToFirstPage() {
        pagination.reset();
    }

    private void updatePageText() {
        var pageNum = "Page " + (currentPage);
        pageText.setText(pageNum);
        pageText.setArgs(Component.translatable("gui.play_maps.page.name", Component.text(pageNum)));
    }
}
