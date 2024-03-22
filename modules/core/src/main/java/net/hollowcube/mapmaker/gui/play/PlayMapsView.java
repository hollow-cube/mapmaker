package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Element;
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
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PlayMapsView extends View {
    private static final PlayerSetting<SortPreset> SORT_PRESET = PlayerSetting.Enum("play_maps.sort_preset", SortPreset.BEST);
    private static final PlayerSetting<Boolean> PARKOUR = PlayerSetting.Bool("play_maps.parkour", false);
    private static final PlayerSetting<Boolean> BUILDING = PlayerSetting.Bool("play_maps.building", false);

    private enum SortPreset {
        APPROVED, BEST, BOOSTED, RECENT, TRENDING;

        public @NotNull String getSortName() {
            return switch (this) {
                case APPROVED -> "quality";
                case BEST -> "best";
                case RECENT -> "new";
                case BOOSTED, TRENDING -> "__IDK__";
            };
        }
    }

    private @ContextObject PlayerService playerService;
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

    private final PlayerDataV2 playerData;


    public PlayMapsView(@NotNull Context context) {
        super(context);
        playerData = PlayerDataV2.fromPlayer(player);

        updateQuery(false);
    }

    // Map type filter

    @Signal(FilterParkourToggle.SIG_TOGGLE)
    private void handleParkourToggle(boolean selected) {
        playerData.setSetting(PARKOUR, selected);
        updateQuery(true);
    }

    @Signal(FilterBuildingToggle.SIG_TOGGLE)
    private void handleBuildingToggle(boolean selected) {
        playerData.setSetting(BUILDING, selected);
        updateQuery(true);
    }

    // Simple preset

    @Signal(SortBestToggle.SIG_TOGGLE)
    private void handleBestToggle(boolean selected) {
        if (!selected) return;

        setSortPreset(SortPreset.BEST);
        updateQuery(true);
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
        if (!selected) return;

        setSortPreset(SortPreset.APPROVED);
        updateQuery(true);
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

        setSortPreset(SortPreset.RECENT);
        updateQuery(true);
    }

    private void setSortPreset(@NotNull SortPreset newPreset) {
        playerData.setSetting(SORT_PRESET, newPreset);
    }

    private void updateQuery(boolean refresh) {
        parkourToggle.setSelected(playerData.getSetting(PARKOUR));
        buildingToggle.setSelected(playerData.getSetting(BUILDING));

        var sortPreset = playerData.getSetting(SORT_PRESET);
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
            var sortPreset = playerData.getSetting(SORT_PRESET);
            boolean parkour = playerData.getSetting(PARKOUR), building = playerData.getSetting(BUILDING);
            var queryResult = mapService.searchMapsV2(player.getUuid().toString(), sortPreset.getSortName(), request.page(), request.pageSize(), building, parkour, query);

            var mapIds = new ArrayList<String>();
            var maps = new ArrayList<MapEntry>();
            for (var map : queryResult.results()) {
                if (map.isCompletable()) mapIds.add(map.id());
                maps.add(new MapEntry(request.context(), map));
            }
            request.respond(maps, queryResult.nextPage());

            maxPages = request.page() + 1;
            currentPage = request.page() + 1;
            updatePageText();

            // Fetch the player's current progress on the maps
            if (mapIds.isEmpty()) return;
            final int page = request.page();
            async(() -> {
                var resp = mapService.getMapProgress(playerData.id(), mapIds);
                player.scheduleNextTick(ignored -> {
                    if (page != pagination.page()) return;
                    pagination.<MapEntry>forEachEntry(page, entry -> {
                        var progress = resp.getProgress(entry.map().id());
                        if (progress != null) entry.setProgress(progress);
                    });
                });
            });
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

    @Signal(Element.SIG_CLOSE)
    private void onClose() {
        playerData.writeUpdatesUpstream(playerService);
    }
}
