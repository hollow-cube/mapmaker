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
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.simple.*;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapQuality;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.util.StringComparison;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class PlayMapsView extends View {
    private static final PlayerSetting<SortPreset> SORT_PRESET = PlayerSetting.Enum("play_maps.sort_preset", SortPreset.BEST);
    private static final PlayerSetting<Boolean> PARKOUR = PlayerSetting.Bool("play_maps.parkour", false);
    private static final PlayerSetting<Boolean> BUILDING = PlayerSetting.Bool("play_maps.building", false);

    public enum SortPreset {
        APPROVED, BEST, BOOSTED, RECENT, TRENDING;

        public MapSearchParams.Builder apply(MapSearchParams.Builder params) {
            return switch (this) {
                case BEST -> params
                        .ascending(false)
                        .best(true)
                        .qualities(MapQuality.GOOD, MapQuality.GREAT, MapQuality.EXCELLENT, MapQuality.OUTSTANDING, MapQuality.MASTERPIECE);
                case RECENT -> params
                        .ascending(false)
                        .best(false);
                case APPROVED -> params
                        .ascending(false)
                        .qualities(MapQuality.GOOD, MapQuality.GREAT, MapQuality.EXCELLENT, MapQuality.OUTSTANDING, MapQuality.MASTERPIECE);
                default -> params;
            };
        }
    }

    private @ContextObject PlayerService playerService;
    private @ContextObject MapService mapService;
    private @ContextObject Player player;
    private @ContextObject String query;

    private @Outlet("title") Text titleText;
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
        this(context, null);
    }

    public PlayMapsView(@NotNull Context context, @Nullable SortPreset startingSort) {
        super(context);
        playerData = PlayerDataV2.fromPlayer(player);

        if (startingSort != null) {
            playerData.setSetting(SORT_PRESET, startingSort);
        }

        titleText.setText("Play Maps");
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
            var variants = new ArrayList<MapVariant>();
            if (playerData.getSetting(PARKOUR)) variants.add(MapVariant.PARKOUR);
            if (playerData.getSetting(BUILDING)) variants.add(MapVariant.BUILDING);

            var params = MapSearchParams.builder(player.getUuid().toString())
                    .page(request.page())
                    .pageSize(request.pageSize())
                    .query(this.query)
                    .variants(variants.toArray(new MapVariant[0]))
                    .ascending(true);
            var response = mapService.searchMaps(sortPreset.apply(params).build());

            // Sort the page of results using string similarity to the query
            var results = response.results();
            if (this.query != null && !this.query.isEmpty()) {
                results = new ArrayList<>(results);
                results.sort(StringComparison.jaroWinkler(query, MapData::name));
            }

            var mapIds = new ArrayList<String>();
            var maps = new ArrayList<MapEntry>();
            for (var map : results) {
                if (map.isCompletable()) mapIds.add(map.id());
                maps.add(new MapEntry(request.context(), map));
            }

            if (this.maxPages == 0 && request.page() == 0) {
                this.maxPages = response.pageCount();
            }

            request.respond(maps, (request.page() + 1) < this.maxPages);

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
                        if (progress != null) entry.setProgress(progress.getKey(), progress.getValue());
                    });
                });
            });
        } catch (Exception e) {
            player.closeInventory();
            player.sendMessage(Component.translatable("generic.unknown_error"));
            ExceptionReporter.reportException(e, player);
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
        FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(playerService));
    }
}
