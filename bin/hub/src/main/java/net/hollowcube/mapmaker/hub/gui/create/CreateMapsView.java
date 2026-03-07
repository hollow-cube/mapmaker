package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.StringComparison;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class CreateMapsView extends Panel {
    private static final int PAGE_SIZE = 5;

    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;

    private final Button createButton;

    // Contains all map slots, including published maps
    private final List<Slot> slots = new ArrayList<>();
    private final Pagination<Unit> pagination;
    private final Text searchTextElement;

    private String searchText = "";
    private @Nullable Runnable remountTask;

    public CreateMapsView(PlayerService playerService, MapService mapService, ServerBridge bridge) {
        super(9, 10);
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;

        background("create_maps2/container", -10, -31);
        add(0, 0, title("Create Map"));

        add(0, 0, backOrClose());

        this.searchTextElement = add(1, 0, new Text("gui.create_maps.search", 7, 1, "Search...")
            .align(7, 5));
        this.searchTextElement.onLeftClick(this::openSearchInput);
        this.searchTextElement.onShiftLeftClick(this::clearSearch);

        this.pagination = add(0, 1, new Pagination<Unit>(9, 5, true).fetchAsync(this::onSearch));
        add(2, 6, this.pagination.prevButton());
        add(3, 6, this.pagination.pageText(3, 1));
        add(6, 6, this.pagination.nextButton());

        this.createButton = add(8, 0, new Button(1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/plus", 1, 1)
            .onLeftClick(() -> this.host.pushTransientView(new NewMapView(mapService, this::acceptNewMap))));
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (isInitial) {
            this.rebuildSlots();
        } else {
            if (this.remountTask != null) {
                this.remountTask.run();
            }
            this.updateCreateButton();
            this.resetSearch();
        }
    }

    private void rebuildSlots() {
        async(() -> {
            var playerId = PlayerData.fromPlayer(this.host.player()).id();

            var remoteSlots = this.mapService.getPlayerMapSlots(playerId);
            var mapBuilderSlots = this.mapService.getMapsPlayerIsBuilderOn(playerId);
            var sortedSlots = this.splitAndSortMaps(remoteSlots, mapBuilderSlots);

            sync(() -> {
                this.slots.clear();
                this.slots.addAll(sortedSlots);
                this.resetSearch();

                this.updateCreateButton();
            });
        });
    }

    private List<Slot> splitAndSortMaps(List<MapData> maps, List<MapData> builderSlots) {
        var unpublished = new ArrayList<Slot>();
        var published = new ArrayList<Slot>();

        for (var map : maps) {
            if (map.isPublished()) {
                published.add(Slot.owned(map));
            } else {
                unpublished.add(Slot.owned(map));
            }
        }

        var result = new ArrayList<>(unpublished);

        for (var map : builderSlots) {
            result.add(Slot.builder(map));
        }

        result.addAll(published);
        return result;
    }

    private void acceptNewMap(MapData slot) {
        // No need to re-sort, we know this should be first in the list.
        this.slots.addFirst(Slot.owned(slot));
        this.resetSearch();
    }

    private void resetSearch() {
        this.pagination.reset(Unit.INSTANCE);
    }

    private void updateCreateButton() {
        // We have to lazy init this as host is not set until the view is mounted
        var unlockedSlots = PlayerData.fromPlayer(this.host.player()).mapSlots();
        var usedSlots = this.getUsedSlots();

        var availableSlots = unlockedSlots - usedSlots;
        if (availableSlots <= 0) {
            // TODO: What do we display if they don't have enough slots?
            this.createButton.translationKey("gui.create_maps.new", 0);
            this.createButton.removeLeftClick();
        } else {
            this.createButton.translationKey("gui.create_maps.new", availableSlots);
        }
    }

    private int getUsedSlots() {
        int count = 0;
        for (var slot : this.slots) {
            if (slot.unpublished()) count++;
        }
        return count;
    }

    private void openSearchInput() {
        this.host.pushView(simpleAnvil(
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
        var results = this.searchText.isEmpty() ? this.slots : this.searchAllSlots();
        if (page == 0) this.pagination.totalPages((int) Math.ceil(((double) results.size()) / PAGE_SIZE));

        var entries = new ArrayList<MapSlotEntry>();
        // Keep as a stream to avoid collecting all to a list just to iterate over it
        for (var slot : this.limitMapSlotsForPage(results, page)) {
            entries.add(new MapSlotEntry(this.playerService, this.mapService, this.bridge, slot.map(), slot.isOwned(),
                () -> this.remountTask = this::rebuildSlots));
        }

        return entries;
    }

    private List<Slot> searchAllSlots() {
        // Always put unpublished slots first
        return Stream.concat(
            Stream.concat(this.searchUnpublishedSlots(), this.searchBuilderSlots()),
            this.searchPublishedSlots()
        ).toList();
    }

    private List<Slot> limitMapSlotsForPage(List<Slot> slots, int page) {
        // Page starts from 0
        return slots.stream()
            .skip((long) page * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .toList();
    }

    private Stream<Slot> searchUnpublishedSlots() {
        return this.slots.stream()
            .filter(Slot::unpublished)
            .filter(slot -> slot.name().contains(this.searchText))
            .sorted(StringComparison.jaroWinkler(this.searchText, Slot::name));
    }

    private Stream<Slot> searchBuilderSlots() {
        return this.slots.stream()
            .filter(Slot::isOwned)
            .filter(slot -> slot.name().contains(this.searchText))
            .sorted(StringComparison.jaroWinkler(this.searchText, Slot::name));
    }

    private Stream<Slot> searchPublishedSlots() {
        return this.slots.stream()
            .filter(Slot::published)
            .filter(slot -> slot.name().contains(this.searchText))
            .sorted(StringComparison.jaroWinkler(this.searchText, Slot::name));
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

    private record Slot(MapData map, boolean isOwned) {
        static Slot owned(MapData map) {
            return new Slot(map, true);
        }

        static Slot builder(MapData map) {
            return new Slot(map, false);
        }

        boolean published() {
            return this.map.isPublished();
        }

        boolean unpublished() {
            return !this.map.isPublished();
        }

        String name() {
            return this.map.name();
        }
    }
}
