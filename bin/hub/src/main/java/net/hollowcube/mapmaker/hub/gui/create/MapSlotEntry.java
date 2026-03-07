package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;

public class MapSlotEntry extends Panel {

    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapData map;
    private final Runnable onPublish;

    public MapSlotEntry(
        ApiClient api, MapService mapService, ServerBridge bridge,
        MapData map, boolean isOwned, Runnable onPublish
    ) {
        super(9, 1);
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;
        this.map = map;
        this.onPublish = onPublish;

        if (map.isPublished()) {
            background("create_maps2/slot/gray", 1, 1);
        } else {
            background("create_maps2/slot/blue", 1, 1);
        }

        var name = map.settings().getNameSafe();

        var iconButton = add(0, 0, new Button(null, 1, 1).onLeftClick(this::onClick));
        var userIcon = map.settings().getIcon();
        if (isOwned) {
            iconButton.translationKey("gui.create_maps.edit.icon");
            if (userIcon != null) {
                iconButton.model(userIcon.toString(), null);
            } else {
                iconButton.sprite("icon2/1_1/item_frame", 1, 1);
            }
        } else {
            iconButton.sprite((Sprite) null)
                .model(MODEL_8X, null)
                .profile(getPlayerHead2d(map.owner()));

            async(() -> {
                var ownerDisplayName = api.players.getDisplayName(map.owner()).asComponent();
                iconButton.translationKey("gui.create_maps.slot.builder", name, ownerDisplayName);
            });
        }

        var textTranslationKey = "gui.create_maps.slot.";
        if (isOwned) {
            if (map.isPublished()) {
                textTranslationKey += "published";
            } else {
                textTranslationKey += "yours";
            }
        } else {
            if (map.verification() != MapVerification.UNVERIFIED) {
                textTranslationKey += "builder.verified";
            } else {
                textTranslationKey += "builder";
            }
        }

        var nameText = new Text(map.isPublished() ? 8 : 7, 1, name)
            .align(2, Text.CENTER)
            .onLeftClick(this::onClick);
        if (isOwned) {
            nameText.translationKey(textTranslationKey, name);
        } else {
            final var translationKey = textTranslationKey;

            async(() -> {
                var ownerDisplayName = api.players.getDisplayName(map.owner()).asComponent();
                nameText.translationKey(translationKey, name, ownerDisplayName);
            });
        }
        add(1, 0, nameText);

        if (!map.isPublished()) {
            add(8, 0, new Button("gui.create_maps.edit.build", 1, 1)
                .sprite("icon2/1_1/hammer", 1, 1)
                .onLeftClickAsync(() -> EditMapView.editMap(mapService, map, this.host, bridge)));
        }
    }

    private void onClick() {
        if (this.map.isPublished()) {
            this.showDetails();
        } else if (isOwner(this.map, this.host.player())) {
            this.editMap();
        } else {
            FutureUtil.submitVirtual(() -> buildMap(this.map, this.host.player(), this.bridge));
        }
    }

    private static boolean isOwner(MapData map, Player player) {
        return player.getUuid().toString().equals(map.owner());
    }

    private void showDetails() {
        var playerId = this.host.player().getUuid().toString();
        async(() -> {
            var playerDisplayName = api.players.getDisplayName(map.owner());
            sync(() -> {
                var view = new MapDetailsView(api, mapService, bridge, map, playerDisplayName, true);
                this.host.pushView(view);
            });
        });
    }

    private void editMap() {
        async(() -> {
            var view = new EditMapView(this.api, this.mapService, this.bridge, this.map, this.onPublish);
            sync(() -> this.host.pushView(view));
        });
    }

    private static void buildMap(MapData map, Player player, ServerBridge bridge) {
        if (map.verification() != MapVerification.UNVERIFIED) {
            player.sendMessage(Component.text("cannot join as map is verified/pending verification"));
        } else {
            EditMapView.beginBuildingMap(bridge, map, player);
        }
    }
}
