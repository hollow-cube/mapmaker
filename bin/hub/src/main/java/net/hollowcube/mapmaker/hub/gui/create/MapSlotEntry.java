package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerService;

public class MapSlotEntry extends Panel {

    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapData map;
    private final Runnable onPublish;

    public MapSlotEntry(PlayerService playerService, MapService mapService, ServerBridge bridge, MapData map, Runnable onPublish) {
        super(9, 1);
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;
        this.map = map;
        this.onPublish = onPublish;

        if (map.isPublished()) {
            background("create_maps2/slot/gray", 1, 1);
        } else {
            background("create_maps2/slot/blue", 1, 1);
        }

        var iconButton = add(0, 0, new Button("gui.create_maps.edit.icon", 1, 1)
            .onLeftClick(this::onClick));
        var userIcon = map.settings().getIcon();
        if (userIcon != null) {
            iconButton.model(userIcon.toString(), null);
        } else {
            iconButton.sprite("icon2/1_1/item_frame", 1, 1);
        }

        var name = map.settings().getNameSafe();
        var textTranslationKey = "gui.create_maps.slot." + (map.isPublished() ? "published" : "yours");
        add(1, 0, new Text(map.isPublished() ? 8 : 7, 1, name)
            .align(2, Text.CENTER)
            .onLeftClick(this::onClick))
            .translationKey(textTranslationKey, name);

        if (!map.isPublished()) {
            add(8, 0, new Button("gui.create_maps.edit.build", 1, 1)
                .sprite("icon2/1_1/hammer", 1, 1)
                .onLeftClickAsync(() -> EditMapView.editMap(map, this.host, bridge)));
        }
    }

    private void onClick() {
        if (this.map.isPublished()) {
            this.showDetails();
        } else {
            this.editMap();
        }
    }

    private void showDetails() {
        var playerId = this.host.player().getUuid().toString();
        async(() -> {
            var playerDisplayName = this.playerService.getPlayerDisplayName2(playerId);
            sync(() -> {
                var view = new MapDetailsView(this.playerService, this.mapService, this.bridge, this.map, playerDisplayName, true);
                this.host.pushView(view);
            });
        });
    }

    private void editMap() {
        async(() -> {
            var view = new EditMapView(this.playerService, this.mapService, this.bridge, this.map, this.onPublish);
            sync(() -> this.host.pushView(view));
        });
    }
}
