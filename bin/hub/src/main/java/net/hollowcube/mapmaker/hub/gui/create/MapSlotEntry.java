package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapSlot;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.util.Sanity;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.confirm;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;

public class MapSlotEntry extends Panel {

    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapSlot slot;
    private final Runnable onPublish;

    private MapSlotEntry(
        ApiClient api, MapService mapService, ServerBridge bridge,
        MapSlot slot, Runnable onPublish
    ) {
        super(9, 1);
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;
        this.slot = slot;
        this.onPublish = onPublish;
    }

    @Blocking
    protected void buildInWorld() {
        if (isOwner(host.player())) {
            EditMapView.editMap(mapService, slot.map(), this.host, bridge);
            return;
        }

        // If you arent the owner, we need to check the latest version to make sure its not in a verifying state
        // TODO: this is still a race, we need to check elsewhere to prevent editing a map during/after verification
        var map = mapService.getMap(host.player().getUuid().toString(), slot.map().id());
        if (map.verification() == MapVerification.PENDING) {
            host.player().sendMessage(Component.translatable("edit.map.failure.verify"));
            return;
        } else if (map.verification() == MapVerification.VERIFIED) {
            host.player().sendMessage(Component.translatable("edit.map.failure.verified"));
            return;
        }

        EditMapView.beginBuildingMap(bridge, map, host.player());
    }

    protected void editMapDetails() {
        Sanity.check(isOwner(host.player()), "cannot edit details for someone else's map");

        async(() -> {
            // TODO: this constructor is blocking, which is kinda confusing and im not a fan overall.
            var view = new EditMapView(this.api, this.mapService, this.bridge, slot, this.onPublish);
            sync(() -> host.pushView(view));
        });
    }

    protected void viewMapDetails() {
        // We only show this for the player themselves anyway so its fine to just get it from them.
        Sanity.check(isOwner(host.player()), "cannot show details for someone else's map");

        var displayName = PlayerData.fromPlayer(host.player()).displayName2();
        var view = new MapDetailsView(api, mapService, bridge, slot.map(), displayName, true);
        this.host.pushView(view);
    }

    protected void removeFromMap() {
        var player = host.player();
        var playerId = PlayerData.fromPlayer(player).id();
        host.pushView(confirm("Leave Map?", () -> FutureUtil.submitVirtual(() -> {
            try {
                api.maps.removeMapBuilder(slot.map().id(), playerId);
                player.closeInventory();
                player.sendMessage(Component.translatable("leave.other.map"));
            } catch (RuntimeException e) {
                ExceptionReporter.reportException(e, playerId);
                player.sendMessage(Component.translatable("generic.unknown_error"));
            }
        })));
    }

    private boolean isOwner(Player player) {
        return player.getUuid().toString().equals(slot.map().owner());
    }

    public static final class Owner extends MapSlotEntry {
        public Owner(
            ApiClient api, MapService mapService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, mapService, bridge, slot, onPublish);

            var map = slot.map();
            var translationKey = "gui.create_maps.slot.yours";
            var mapName = map.settings().getNameSafe();

            // TODO: If ready to publish, green background
            background("create_maps2/slot/blue", 1, 1);

            var iconButton = add(0, 0, new Button(null, 1, 1)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::editMapDetails));
            var userIcon = map.settings().getIcon();
            if (userIcon != null) iconButton.model(userIcon.toString(), null);
            else iconButton.sprite("icon2/1_1/item_frame", 1, 1);

            add(1, 0, new Text(7, 1, mapName)
                .align(2, Text.CENTER)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::editMapDetails));

            add(8, 0, new Button("gui.create_maps.slot_selection.quick_build", 1, 1)
                .sprite("icon2/1_1/hammer", 1, 1)
                .onLeftClickAsync(this::buildInWorld));
        }
    }

    public static final class Builder extends MapSlotEntry {
        public Builder(
            ApiClient api, MapService mapService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, mapService, bridge, slot, onPublish);

            var map = slot.map();
            var translationKey = "gui.create_maps.slot.yours";
            var mapName = map.settings().getNameSafe();

            // TODO: Different background for builder maps
            background("create_maps2/slot/blue", 1, 1);

            add(0, 0, new Button(null, 1, 1)
                .translationKey(translationKey, mapName)
                .background("create_maps2/head_outline", 4, 4)
                .profile(getPlayerHead2d(map.owner()))
                .model(MODEL_8X, null)
                .onLeftClickAsync(this::buildInWorld));
////            new Button(null, 1, 1)
////                .background("create_maps2/head_outline" + (pending ? "_pending" : ""), 4, 4)
////                .model(MODEL_8X, null)
////                .profile(getPlayerHead2d(builderId))
////                .translationKey("gui.create_maps.edit.builders." + (pending ? "pending" : "entry"), displayName.asComponent())
//            iconButton
//                .background("create_maps2/head_outline", 4, 4)
//                .model(MODEL_8X, null)
//                .profile(getPlayerHead2d(map.owner()));
//
//            async(() -> {
//                var ownerDisplayName = api.players.getDisplayName(map.owner()).asComponent();
//                iconButton.translationKey("gui.create_maps.slot.builder", name, ownerDisplayName);
//            });

//            var iconButton = add(0, 0, new Button(null, 1, 1)
//                .background("create_maps2/head_outline" + (pending ? "_pending" : ""), 4, 4)
//                .translationKey(translationKey, mapName)
//                .onLeftClick(this::editMapDetails));
//            var userIcon = map.settings().getIcon();
//            if (userIcon != null) iconButton.model(userIcon.toString(), null);
//            else iconButton.sprite("icon2/1_1/item_frame", 1, 1);

            add(1, 0, new Text(7, 1, mapName)
                .align(2, Text.CENTER)
                .translationKey(translationKey, mapName)
                .onLeftClickAsync(this::buildInWorld));

            add(8, 0, new Button("gui.create_maps.slot.other.leave", 1, 1)
                .sprite("icon2/1_1/running_out_door", 1, 1)
                .onLeftClick(this::removeFromMap));
        }
    }

    public static final class Published extends MapSlotEntry {
        public Published(
            ApiClient api, MapService mapService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, mapService, bridge, slot, onPublish);

            var map = slot.map();
            var translationKey = "gui.create_maps.slot.published";
            var mapName = map.settings().getNameSafe();

            background("create_maps2/slot/gray", 1, 1);

            var iconButton = add(0, 0, new Button(null, 1, 1)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::viewMapDetails));
            var userIcon = map.settings().getIcon();
            if (userIcon != null) iconButton.model(userIcon.toString(), null);
            else iconButton.sprite("icon2/1_1/item_frame", 1, 1);

            add(1, 0, new Text(8, 1, mapName)
                .align(2, Text.CENTER)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::viewMapDetails));
        }
    }
}
