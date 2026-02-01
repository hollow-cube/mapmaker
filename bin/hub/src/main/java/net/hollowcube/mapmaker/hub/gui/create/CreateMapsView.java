package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSlot;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.util.StringComparison;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class CreateMapsView extends Panel {
    public static final List<Component> LORE_POSTFIX_CLICKSELECT = LanguageProviderV2.translateMulti("gui.action.clickselect", List.of());
    private static final Logger log = LoggerFactory.getLogger(CreateMapsView.class);

    private final MapService mapService;
    private final ServerBridge bridge;

    private final Button createButton;

    private final List<MapSlot> slots = new ArrayList<>();
    private final Pagination<Unit> pagination;
    private final Text searchTextElement;

    private String searchText = "";

    public CreateMapsView(MapService mapService, ServerBridge bridge) {
        super(9, 10);
        this.mapService = mapService;
        this.bridge = bridge;

        background("create_maps2/container", -10, -31);
        add(0, 0, title("Create Map"));

        add(0, 0, backOrClose());

        this.searchTextElement = add(1, 0, new Text("gui.create_maps.search", 7, 1, "Search...")
            .align(7, 5));
        this.searchTextElement.onLeftClick(this::openSearchInput);
        this.searchTextElement.onShiftLeftClick(this::clearSearch);

        this.pagination = add(0, 1, new Pagination<Unit>(9, 5)
            .fetchAsync(this::onSearch));
        add(2, 6, this.pagination.prevButton());
        add(3, 6, this.pagination.pageText(3, 1));
        add(6, 6, this.pagination.nextButton());

        this.createButton = add(8, 0, new Button(1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/plus", 1, 1)
            .onLeftClick(() -> host.pushTransientView(new NewMapView(mapService, this::acceptNewMap))));
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (isInitial) {
            // full load from server
            async(() -> {
                var playerId = PlayerData.fromPlayer(host.player()).id();
                var remoteSlots = mapService.getPlayerMapSlots(playerId);

                sync(() -> {
                    this.updateCreateButton();

                    slots.clear();
                    slots.addAll(remoteSlots);
                    slots.sort((MapSlot a, MapSlot b) -> b.createdAt().compareTo(a.createdAt()));
                    this.resetSearch();
                });
            });
        } else {
            // Update from changes (name/icon generally)
            this.resetSearch();
        }
    }

    private void acceptNewMap(MapSlot slot) {
        // No need to re-sort, we know this should be first in the list.
        slots.addFirst(slot);
        this.resetSearch();
    }

    private void resetSearch() {
        this.pagination.reset(Unit.INSTANCE);
    }

    private void updateCreateButton() {
        // We have to lazy init this as host is not set until the view is mounted
        var data = MapPlayerData.fromPlayer(this.host.player());
        var unusedSlots = data.unlockedSlots() - this.slots.size();
        this.createButton.translationKey("gui.create_maps.new", unusedSlots);
    }

    private void openSearchInput() {
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "map_browser/search_anvil_icon",
            "Search Created Maps",
            this::handleSearchTextChange
        ));
    }

    private void clearSearch() {
        handleSearchTextChange("");
    }

    @Blocking
    private List<? extends Panel> onSearch(Unit unused, int page, int pageSize) {
        var slotResults = this.searchMapSlots();

        List<MapData> publishResults = List.of();
        if (!this.searchText.isEmpty()) {
            // Search for published maps if we've searched for something - else don't bother
            var params = MapSearchParams.builder(this.host.player().getUuid().toString()).query(this.searchText);

            var response = this.mapService.searchMaps(params.page(page).pageSize(pageSize).build());
            publishResults = response.results();

            if (page == 0) {
                var newPageCount = calculateTotalPages(response.pageCount(), slotResults.size(), publishResults.size(), pageSize);
                this.pagination.totalPages(newPageCount);
            }

            if (!this.searchText.isEmpty()) {
                publishResults = new ArrayList<>(publishResults);
                publishResults.sort(StringComparison.jaroWinkler(this.searchText, MapData::name));
            }
        }

        var entries = new ArrayList<MapSlotEntry>();
        // Always put unpublished maps in slots first before published maps
        for (var slot : slotResults) {
            entries.add(new MapSlotEntry(this.mapService, this.bridge, slot.map()));
        }
        for (var map : publishResults) {
            entries.add(new MapSlotEntry(this.mapService, this.bridge, map));
        }

        return entries;
    }

    private List<MapSlot> searchMapSlots() {
        if (this.searchText.isEmpty()) return this.slots;

        var results = new ArrayList<MapSlot>();
        for (MapSlot slot : this.slots) {
            var name = slot.map().name();
            if (name.contains(this.searchText)) results.add(slot);
        }

        results.sort(StringComparison.jaroWinkler(this.searchText, slot -> slot.map().name()));
        return results;
    }

    private static int calculateTotalPages(int pages, int slotsCount, int publishedCount, int publishedPageSize) {
        var entriesOnLastPage = publishedCount % publishedPageSize;
        if (entriesOnLastPage + slotsCount > publishedPageSize) {
            return pages + 1;
        } else {
            return pages;
        }
    }

    private void handleSearchTextChange(String newValue) {
        var oldValue = this.searchText;
        this.searchText = newValue.trim();
        if (this.searchText.equals(oldValue)) return;

        var translationKey = "gui.create_maps.search";
        this.searchTextElement.translationKey(translationKey);
        this.searchTextElement.text(this.searchText.isEmpty() ? "Search..." : this.searchText);
        this.pagination.reset();
    }
}
