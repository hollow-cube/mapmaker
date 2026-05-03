package net.hollowcube.mapmaker.gui.map.details;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.lang.TimeComponent;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.map.MapListView;
import net.hollowcube.mapmaker.gui.map.MapReportView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Blocking;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.info;
import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

public class MapDetailsView extends Panel {
    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapData map;
    private final boolean showJoinButton;

    private final Button playButton;

    @Blocking
    public MapDetailsView(
        ApiClient api, MapService mapService, ServerBridge bridge,
        MapData mapData, boolean showJoinButton
    ) {
        var displayName = api.players.getDisplayName(mapData.owner());
        this(api, mapService, bridge, mapData, displayName, showJoinButton);
    }

    public MapDetailsView(
        ApiClient api, MapService mapService, ServerBridge bridge,
        MapData mapData, DisplayName authorName, boolean showJoinButton
    ) {
        super(9, 10);
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;
        this.map = mapData;
        this.showJoinButton = showJoinButton;

        background("map_details/container", -10, -32);
        add(0, 0, new Text("", 9, 0, mapData.name())
            .align(30, -23));
        var lowerVariant = mapData.settings().getVariant().toString().toLowerCase(Locale.ROOT);
        add(0, 0, new Button("", 0, 0)
            .sprite("map_details/variant_" + lowerVariant, -5, -27));

        add(0, 0, backOrClose());
        add(1, 0, info("map_browser").onLeftClickAsync(this::showMapInformation));
        var authorUsername = Objects.requireNonNullElse(authorName.getUsername(), "Unknown");
        add(2, 0, new Text("", 5, 1, authorUsername)
            .align(Text.CENTER, Text.CENTER)
            .background("generic2/btn/default/5_1")
            .translationKey("gui.map_details.creator_profile", authorName.build()))
            .onLeftClick(() -> host.pushView(new MapListView.Player(api, mapService, bridge, map.owner())));
        add(7, 0, new Button("gui.map_rating.report_map", 2, 1)
            .background("generic2/btn/default/2_1")
            .sprite("map_details/action/report", 15, 3)
            .onLeftClick(() -> host.pushView(new MapReportView(api.maps, map))));

        var tabs = add(0, 2, new Switch(9, 4, List.of(
            new MapDetailsInfoPanel(mapData),
            new MapDetailsTimesPanel(api, mapService, mapData),
            new MapDetailsRatePanel(api.maps, mapData.id())
        )));
        tabs.select(0);
        add(0, 1, tabs.button(0, 3, 1,
            "gui.map_details.info.tab", "map_details/info/tab"));
        add(3, 1, tabs.button(1, 3, 1,
            "gui.map_details.times.tab", "map_details/times/tab"));
        add(6, 1, tabs.button(2, 3, 1,
            "gui.map_details.rate.tab", "map_details/rate/tab"));

        add(0, 6, new Button("gui.map_details.map_info.boost_map", 3, 3)
            .sprite("map_details/action/boost"));
        this.playButton = add(3, 6, new Button("gui.map_details." + (showJoinButton ? "play_map" : "leave_map"), 3, 3)
            .sprite("map_details/action/" + (showJoinButton ? "play" : "leave"))
            .onLeftClickAsync(showJoinButton ? this::handleJoinMap : this::handleLeaveMap));
        add(6, 6, new Button("gui.map_details.suggest_similar_maps", 3, 3)
            .sprite("map_details/action/similar"));
    }

    private void handleJoinMap() {
        FutureUtil.submitVirtual(
            () -> this.bridge.joinMap(host.player(), map.id(), ServerBridge.JoinMapState.PLAYING, "map_details_gui")
        );
    }

    private void handleLeaveMap() {
        FutureUtil.submitVirtual(
            () -> this.bridge.joinHub(host.player())
        );
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (!isInitial || !showJoinButton) return;

        // Fetch the latest save state to show in play button
        async(() -> {
            try {
                var playerId = PlayerData.fromPlayer(host.player()).id();
                var saveState = mapService.getLatestSaveState(map.id(), playerId, SaveStateType.PLAYING, null);

                playButton.translationKey("gui.map_details.continue_map", formatMapPlaytime(saveState.getPlaytime(), true));
            } catch (MapService.NotFoundError ignored) {
                // Its ok, leave as default key
            }
        });
    }

    private void showMapInformation() {
        var player = host.player();
        player.closeInventory();

        Component authorName;
        try {
            authorName = api.players.getDisplayName(map.owner()).build();
        } catch (Throwable t) {
            ExceptionReporter.reportException(t, player);
            authorName = Component.text("Unknown", NamedTextColor.RED);
        }
        player.sendMessage(LanguageProviderV2.translateMultiMerged("gui.map_details.map_info_tab.published_id", List.of(
            Component.text(map.id()),
            Component.text(Objects.requireNonNullElse(map.publishedIdString(), "None/Not Published")),
            Component.text(map.name()),
            authorName,
            TimeComponent.of(map.publishedAt())
        )));
    }
}
