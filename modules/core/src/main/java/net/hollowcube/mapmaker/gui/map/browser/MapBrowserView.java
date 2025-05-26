package net.hollowcube.mapmaker.gui.map.browser;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.gui.map.MapIconPanel;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Pagination;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.StringComparison;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class MapBrowserView extends Panel {

    public enum SortPreset {
        BEST, QUALITY, NEW
    }

    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;

    private final Pagination<MapSearchParams.Builder> pagination;
    private final Text searchTextElement;
    private final SimpleSortPanel simpleSortPanel;

    private volatile String searchText = "";

    public MapBrowserView(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull ServerBridge bridge) {
        this(playerService, mapService, bridge, true);
    }

    public MapBrowserView(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull ServerBridge bridge, boolean fetchOnMount) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;

        background("map_browser/container", -10, -31);
        add(0, 0, title("Play Maps"));

        add(0, 0, backOrClose());
        this.searchTextElement = add(1, 0, new Text("gui.map_browser.search_maps.empty", 8, 1, "Search...")
                .align(8, 5));
        this.searchTextElement.onLeftClick(this::openSearchInput);
        this.searchTextElement.onShiftLeftClick(this::handleSearchClear);

        this.pagination = add(1, 2, new Pagination<MapSearchParams.Builder>(7, 3)
                .fetchAsync(this::onSearch));
        add(2, 5, pagination.prevButton());
        add(3, 5, pagination.pageText(3, 1));
        add(6, 5, pagination.nextButton());

        // !!! WARNING !!!
        // Once we support advanced search, the simpleSort function needs to force the ui into simple mode.
        this.simpleSortPanel = add(0, 6, new SimpleSortPanel(this::handleSortChange, fetchOnMount));
    }

    // This is a pretty gross inflexible method, but its fine for now
    public void simpleSort(@NotNull SortPreset preset) {
        this.simpleSortPanel.setSync(false);
        this.simpleSortPanel.setSort(preset);
    }

    public void openSearchInput() {
        host.pushView(simpleAnvil(
                "generic2/anvil/field_container",
                "map_browser/search_anvil_icon",
                "Search Maps by Name",
                this::handleSearchTextChange
        ));
    }

    @Blocking
    private @NotNull List<? extends Panel> onSearch(@NotNull MapSearchParams.Builder params, int page, int pageSize) {
        // If we have a search query, ignore the given params.
        if (!this.searchText.isEmpty()) {
            params = MapSearchParams.builder(host.player().getUuid().toString())
                    .query(searchText);
        }

        var response = mapService.searchMaps(params.page(page).pageSize(pageSize).build());
        if (page == 0) pagination.totalPages(response.pageCount());

        // Sort the page of results using string similarity to the query
        var results = response.results();
        if (!this.searchText.isEmpty()) {
            results = new ArrayList<>(results);
            results.sort(StringComparison.jaroWinkler(searchText, MapData::name));
        }

        var mapIds = new ArrayList<String>();
        var entries = new ArrayList<MapIconPanel>();
        for (var map : results) {
            if (map.isCompletable()) mapIds.add(map.id());
            entries.add(new MapIconPanel(playerService, mapService, bridge, map));
        }

        // Fetch the player's current progress on the maps
        if (!mapIds.isEmpty()) async(() -> {
            var resp = mapService.getMapProgress(host.player().getUuid().toString(), mapIds);
            sync(() -> {
                for (var map : entries) {
                    var progress = resp.getProgress(map.map().id());
                    if (progress != null) map.updateProgress(progress);
                }
            });
        });

        return entries;
    }

    @Override
    protected void unmount() {
        var playerData = PlayerDataV2.fromPlayer(host.player());
        FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(playerService));

        super.unmount();
    }

    private void handleSortChange(@NotNull MapSearchParams.Builder params) {
        // Altering the sort params will reset any search query to use the search you input.
        handleSearchTextChange("");
        this.pagination.reset(params);
    }

    private void handleSearchClear() {
        handleSearchTextChange("");
    }

    private void handleSearchTextChange(@NotNull String newValue) {
        var oldValue = this.searchText;
        searchText = newValue.trim();
        if (searchText.equals(oldValue)) return;

        this.searchTextElement.translationKey(searchText.isEmpty() ? "gui.map_browser.search_maps.empty"
                : "gui.map_browser.search_maps", searchText);
        this.searchTextElement.text(searchText.isEmpty() ? "Search..." : searchText);
        this.pagination.reset();
    }

}
