package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSlot;
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
    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;

    private final Button createButton;

    // Contains all map slots, including published maps
    private final List<MapSlot> slots = new ArrayList<>();
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

        this.pagination = add(0, 1, new Pagination<Unit>(9, 5)
            .fetchAsync(this::onSearch));
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
            this.resetSearch();
        }
    }

    private void rebuildSlots() {
        async(() -> {
            var playerId = PlayerData.fromPlayer(this.host.player()).id();
            var remoteSlots = this.mapService.getPlayerMapSlots(playerId);

            sync(() -> {
                this.slots.clear();
                this.slots.addAll(remoteSlots);
                this.slots.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
                this.resetSearch();

                this.updateCreateButton();
            });
        });
    }

    private void acceptNewMap(MapSlot slot) {
        // No need to re-sort, we know this should be first in the list.
        this.slots.addFirst(slot);
        this.resetSearch();
    }

    private void resetSearch() {
        this.pagination.reset(Unit.INSTANCE);
    }

    private void updateCreateButton() {
        // We have to lazy init this as host is not set until the view is mounted
        var unlockedSlots = MapPlayerData.fromPlayer(this.host.player()).unlockedSlots();
        var usedSlots = this.slots.stream().filter(slot -> !slot.map().isPublished()).count();
        this.createButton.translationKey("gui.create_maps.new", unlockedSlots - usedSlots);
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

        var entries = new ArrayList<MapSlotEntry>();
        // Always put unpublished maps in slots first before published maps
        for (var slot : results) {
            entries.add(new MapSlotEntry(this.playerService, this.mapService, this.bridge, slot.map(),
                                         () -> this.remountTask = this::rebuildSlots));
        }

        return entries;
    }

    private List<MapSlot> searchAllSlots() {
        // Always put unpublished slots first
        return Stream.concat(this.searchUnpublishedSlots(), this.searchPublishedSlots()).toList();
    }

    private Stream<MapSlot> searchUnpublishedSlots() {
        return this.slots.stream()
            .filter(slot -> !slot.map().isPublished())
            .filter(slot -> slot.map().name().contains(this.searchText))
            .sorted(StringComparison.jaroWinkler(this.searchText, slot -> slot.map().name()));
    }

    private Stream<MapSlot> searchPublishedSlots() {
        return this.slots.stream()
            .filter(slot -> slot.map().isPublished())
            .filter(slot -> slot.map().name().contains(this.searchText))
            .sorted(StringComparison.jaroWinkler(this.searchText, slot -> slot.map().name()));
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
