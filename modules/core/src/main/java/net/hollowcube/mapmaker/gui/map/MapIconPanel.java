package net.hollowcube.mapmaker.gui.map;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapIconPanel extends Panel {
    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapData map;

    private DisplayName authorName = null; // Filled async
    private Map.Entry<PersonalizedMapData.Progress, Integer> progress = null; // Filled async

    private final Button button;

    public MapIconPanel(
            @NotNull PlayerService playerService, @NotNull MapService mapService,
            @NotNull ServerBridge bridge, @NotNull MapData map
    ) {
        super(1, 1);
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;
        this.map = map;

        this.button = add(0, 0, new Button(null, 1, 1)
                .onLeftClickAsync(this::handlePlayMap)
                .onShiftLeftClickAsync(this::handlePlayMap) // for old shift+click behavior muscle memory
                .onRightClick(this::handleViewMapDetails));
    }

    public @NotNull MapData map() {
        return map;
    }

    private void handlePlayMap() {
        var player = host.player();

        // Don't try to join if the player is on the wrong version. Bridge will also check this, but it
        // sends a message that we don't want to show in this case.
        var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
        if (playerProtocolVersion < map.protocolVersion()) return;

        player.closeInventory();
        bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.PLAYING, "play_maps_gui");
    }

    private void handleViewMapDetails() {
        if (this.authorName == null) return;
        host.pushView(new MapDetailsView(playerService, mapService, bridge, map, authorName, true));
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (this.authorName != null) return;
        async(() -> updateAuthor(playerService.getPlayerDisplayName2(map.owner())));
    }

    private void updateAuthor(@NotNull DisplayName displayName) {
        sync(() -> {
            authorName = displayName;
            updateIcon();
        });
    }

    public void updateProgress(@NotNull Map.Entry<PersonalizedMapData.Progress, Integer> progress) {
        sync(() -> {
            this.progress = progress;
            updateIcon();
        });
    }

    private void updateIcon() {
        var icon = Objects.requireNonNullElse(map.settings().getIcon(), Material.PAPER);
        button.model(icon.name(), null);

        var authorName = OpUtils.mapOr(this.authorName, DisplayName::build, Component.text("loading"));
        var playerProtocolVersion = ProtocolVersions.getProtocolVersion(host.player());
        var entry = MapData.createHoverComponents(map, authorName, progress, playerProtocolVersion); // todo

        if (playerProtocolVersion < map.protocolVersion()) {
            entry.getValue().addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display.wrongversion.footer", List.of()));
        } else {
            entry.getValue().addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display.footer", List.of()));
        }
        button.text(entry.getKey(), entry.getValue());
    }
}
