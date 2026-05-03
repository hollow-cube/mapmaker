package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapRole;
import net.hollowcube.mapmaker.api.maps.MapSlot;
import net.hollowcube.mapmaker.gui.store.StoreHelpers;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.util.StringComparison;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class CreateMapsView extends Panel {
    private static final int PAGE_SIZE = 5;

    public static void open(Player player, ApiClient api, PlayerService playerService, ServerBridge bridge) {
        var playerId = PlayerData.fromPlayer(player).id();
        var slots = api.maps.getPlayerSlots(playerId).results();

        if (slots.isEmpty()) {
            Panel.open(player, new NewMapView(api.maps, playerService, _ -> FutureUtil.submitVirtual(() -> open(player, api, playerService, bridge))));
        } else {
            Panel.open(player, new CreateMapsView(api, playerService, bridge, slots));
        }
    }

    private final ApiClient api;
    private final PlayerService playerService;
    private final ServerBridge bridge;

    private final Button createButton;

    // Contains all map slots, including published maps
    private final List<MapSlot> slots = new ArrayList<>();
    private final Pagination<Unit> pagination;
    private final Text searchTextElement;

    private String searchText = "";
    private @Nullable Runnable remountTask;

    public CreateMapsView(ApiClient api, PlayerService playerService, ServerBridge bridge, List<MapSlot> initialSlots) {
        super(9, 10);
        this.api = api;
        this.playerService = playerService;
        this.bridge = bridge;
        this.slots.addAll(initialSlots);

        background("create_maps2/container", -10, -31);
        add(0, 0, title("Create Maps"));

        add(0, 0, backOrClose());

        this.searchTextElement = add(1, 0, new Text("gui.create_maps.search", 7, 1, "Search...")
            .align(7, 5));
        this.searchTextElement.onLeftClick(this::openSearchInput);
        this.searchTextElement.onShiftLeftClick(this::clearSearch);

        this.pagination = add(0, 1, new Pagination<>(9, 5, Unit.INSTANCE)
            .fetch(this::onSearch));
        add(2, 6, this.pagination.prevButton());
        add(3, 6, this.pagination.pageText(3, 1));
        add(6, 6, this.pagination.nextButton());

        this.createButton = add(8, 0, new Button(1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/plus", 1, 1)
            .onLeftClick(this::createMapOrOpenStore)
            .onRightClick(this::tryOpenSecondaryStore));

        pagination.renderSync();
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (isInitial) {
            this.updateCreateButton();
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
            var slots = this.api.maps.getPlayerSlots(playerId).results();

            sync(() -> {
                this.slots.clear();
                this.slots.addAll(slots);
                this.resetSearch();

                this.updateCreateButton();
            });
        });
    }

    private void acceptNewMap(MapData map) {
        // No need to re-sort, we know this should be first in the list.
        this.slots.addFirst(new MapSlot(map, Instant.now(), MapRole.OWNER, List.of()));
        this.resetSearch();
    }

    private void resetSearch() {
        this.pagination.reset();
    }

    private void updateCreateButton() {
        // We have to lazy init this as host is not set until the view is mounted

        int availableSlots = getAvailableSlots();
        if (availableSlots > 0) {
            this.createButton.translationKey("gui.create_maps.new", availableSlots);
            return;
        }

        // No remaining slots, prompt to unlock more in the store with hypercube first, or cubits if they already have hypercube.
        var playerData = PlayerData.fromPlayer(this.host.player());
        boolean isHypercube = playerData.isHypercube();
        boolean hasCubits = playerData.cubits() >= 50;

        String hypercubeKey = isHypercube ? "has_hypercube" : "no_hypercube";
        String cubitsKey = hasCubits ? "has_cubits" : "no_cubits";
        this.createButton.translationKey("gui.create_maps.new.no_space." + hypercubeKey + "." + cubitsKey);
    }

    private void createMapOrOpenStore() {
        int availableSlots = getAvailableSlots();
        if (availableSlots > 0) {
            host.pushTransientView(new NewMapView(api.maps, playerService, this::acceptNewMap));
            return;
        }

        var playerData = PlayerData.fromPlayer(this.host.player());
        if (playerData.cubits() >= ShopUpgrade.MAP_BUILDER_2.cubits()) {
            host.pushView(confirm("Buy Map Slot?", FutureUtil.virtual(this::handleBuyMapSlot)));
        } else if (playerData.isHypercube()) {
            this.host.pushView(new StoreView(playerService, StoreView.TAB_CUBITS));
        } else {
            this.host.pushView(new StoreView(playerService, StoreView.TAB_HYPERCUBE));
        }
    }

    private void tryOpenSecondaryStore() {
        int availableSlots = getAvailableSlots();
        if (availableSlots > 0) return;

        var playerData = PlayerData.fromPlayer(this.host.player());
        if (playerData.isHypercube()) return;

        var secondaryTab = playerData.cubits() >= ShopUpgrade.MAP_BUILDER_2.cubits() ? StoreView.TAB_HYPERCUBE : StoreView.TAB_CUBITS;
        this.host.pushView(new StoreView(playerService, secondaryTab));
    }

    @Blocking
    private void handleBuyMapSlot(Player player) {
        StoreHelpers.buyUpgrade(playerService, player, ShopUpgrade.MAP_SLOT);
        sync(() -> {
            updateCreateButton();

            // If they now have an available slot (which is not always the case
            // if they have over-created maps and lost slots), move to create screen.
            if (getAvailableSlots() > 0)
                createMapOrOpenStore();
        });
    }

    private int getAvailableSlots() {
        var playerData = PlayerData.fromPlayer(this.host.player());
        var unlockedSlots = playerData.mapSlots();
        var usedSlots = this.getUsedSlots();
        return unlockedSlots - usedSlots;
    }

    private int getUsedSlots() {
        int count = 0;
        for (var slot : this.slots) {
            if (slot.map().isPublished())
                continue;
            count++;
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

    private List<? extends Panel> onSearch(Unit unused, int page, int pageSize) {
        final var results = new ArrayList<>(slots);

        if (!searchText.isEmpty())
            results.sort(StringComparison.jaroWinkler(this.searchText, slot -> slot.map().name()));
        this.pagination.totalPages((int) Math.ceil(((double) results.size()) / PAGE_SIZE));

        var entries = new ArrayList<MapSlotEntry>();
        for (int i = page * PAGE_SIZE; i < (page + 1) * PAGE_SIZE && i < results.size(); i++) {
            final var slot = results.get(i);
            Runnable onPublish = () -> this.remountTask = this::rebuildSlots;

            if (slot.map().isPublished()) {
                entries.add(new MapSlotEntry.Published(this.api, this.playerService, this.bridge, slot, onPublish));
            } else if (slot.role() == MapRole.OWNER) {
                entries.add(new MapSlotEntry.Owner(this.api, this.playerService, this.bridge, slot, onPublish));
            } else {
                entries.add(new MapSlotEntry.Builder(this.api, this.playerService, this.bridge, slot, onPublish));
            }
        }

        return entries;
    }

    private void handleSearchTextChange(String newValue) {
        var oldValue = this.searchText;
        this.searchText = newValue.trim();
        if (this.searchText.equals(oldValue)) return;

        var translationKey = this.searchText.isEmpty() ? "gui.create_maps.search" : "gui.create_maps.search.with_data";
        this.searchTextElement.translationKey(translationKey);
        this.searchTextElement.text(this.searchText.isEmpty() ? "Search..." : this.searchText);
        this.pagination.reset();
    }

}
