package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.ipc.map.MapSlot;
import net.hollowcube.ipc.map.MapVerification;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapWriteMessages;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.AccountService;
import net.hollowcube.mapmaker.util.Sanity;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;

import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.confirm;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;
import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class MapSlotEntry extends Panel {

    private final ApiClient api;
    private final AccountService accountService;
    private final ServerBridge bridge;
    private final MapSlot slot;
    private final Runnable onPublish;
    private final Consumer<MapSlot> onEdit;

    private MapSlotEntry(
        ApiClient api,
        AccountService accountService, ServerBridge bridge,
        MapSlot slot, Runnable onPublish, Consumer<MapSlot> onEdit
    ) {
        super(9, 1);
        this.api = api;
        this.accountService = accountService;
        this.bridge = bridge;
        this.slot = slot;
        this.onPublish = onPublish;
        this.onEdit = onEdit;
    }

    @Blocking
    protected void buildInWorld() {
        if (isOwner(host.player())) {
            EditMapView.editMap(api.maps, slot.map(), this.host, bridge);
            return;
        }

        // If you arent the owner, we need to check the latest version to make sure its not in a verifying state
        // TODO: this is still a race, we need to check elsewhere to prevent editing a map during/after verification
        var map = api.maps.get(slot.map().id().toString());
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
            var view = new EditMapView(this.api, this.accountService, this.bridge, slot, this.onPublish, this.onEdit);
            sync(() -> host.pushView(view));
        });
    }

    protected void viewMapDetails() {
        // We only show this for the player themselves anyway so its fine to just get it from them.
        Sanity.check(isOwner(host.player()), "cannot show details for someone else's map");

        var displayName = localPlayer(host.player()).info().displayName();
        var view = new MapDetailsView(api, bridge, slot.map(), displayName, true);
        this.host.pushView(view);
    }

    protected void removeFromMap() {
        var player = host.player();
        var playerId = localPlayer(player).id().toString();
        host.pushView(confirm("Leave Map?", () -> FutureUtil.submitVirtual(() -> {
            try {
                var result = api.maps.removeMapBuilder(slot.map().id().toString(), playerId);
                if (!result.succeeded()) {
                    player.sendMessage(MapWriteMessages.failure(result));
                    return;
                }
                player.closeInventory();
                player.sendMessage(Component.translatable("leave.other.map"));
            } catch (RuntimeException e) {
                ExceptionReporter.reportException(e, playerId);
                player.sendMessage(Component.translatable("generic.unknown_error"));
            }
        })));
    }

    private boolean isOwner(Player player) {
        return player.getUuid().toString().equals(slot.map().owner().toString());
    }

    public static final class Owner extends MapSlotEntry {
        public Owner(
            ApiClient api,
            AccountService accountService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish, Consumer<MapSlot> onEdit
        ) {
            super(api, accountService, bridge, slot, onPublish, onEdit);

            var map = slot.map();
            var translationKey = "gui.create_maps.slot.yours";
            var mapName = MapSettings.getNameSafe(map.settings());

            // TODO: If ready to publish, green background
            background("create_maps2/slot/blue", 1, 1);

            var iconButton = add(0, 0, new Button(null, 1, 1)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::editMapDetails));
            var userIcon = MapSettings.getIcon(map.settings());
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
        private static final String TRANSLATION_KEY = "gui.create_maps.slot.other";
        private static final Component LOADING = Component.text("loading...");

        private final ApiClient api;
        private final MapSlot slot;

        private final Button iconButton;
        private final Button nameButton;

        public Builder(
            ApiClient api,
            AccountService accountService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish, Consumer<MapSlot> onEdit
        ) {
            super(api, accountService, bridge, slot, onPublish, onEdit);
            this.api = api;
            this.slot = slot;

            var map = slot.map();
            var mapName = MapSettings.getNameSafe(map.settings());

            // TODO: Different background for builder maps
            background("create_maps2/slot/blue", 1, 1);

            this.iconButton = add(0, 0, new Button(null, 1, 1)
                .translationKey(TRANSLATION_KEY, mapName, LOADING)
                .background("create_maps2/head_outline", 4, 4)
                .profile(getPlayerHead2d(map.owner()))
                .model(MODEL_8X, null)
                .onLeftClickAsync(this::buildInWorld));

            this.nameButton = add(1, 0, new Text(7, 1, mapName)
                .align(2, Text.CENTER)
                .translationKey(TRANSLATION_KEY, mapName, LOADING)
                .onLeftClickAsync(this::buildInWorld));

            add(8, 0, new Button("gui.create_maps.slot.other.leave", 1, 1)
                .sprite("icon2/1_1/running_out_door", 1, 1)
                .onLeftClick(this::removeFromMap));
        }

        @Override
        protected void mount(InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);

            if (!isInitial) return;
            async(() -> {
                var ownerDisplayName = api.players.displayName(slot.map().owner()).render();
                sync(() -> {
                    var mapName = MapSettings.getNameSafe(slot.map().settings());

                    this.iconButton.translationKey(TRANSLATION_KEY, mapName, ownerDisplayName);
                    this.nameButton.translationKey(TRANSLATION_KEY, mapName, ownerDisplayName);
                });
            });

        }
    }

    public static final class Published extends MapSlotEntry {
        public Published(
            ApiClient api,
            AccountService accountService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish, Consumer<MapSlot> onEdit
        ) {
            super(api, accountService, bridge, slot, onPublish, onEdit);

            var map = slot.map();
            var translationKey = "gui.create_maps.slot.published";
            var mapName = MapSettings.getNameSafe(map.settings());

            background("create_maps2/slot/gray", 1, 1);

            var iconButton = add(0, 0, new Button(null, 1, 1)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::viewMapDetails));
            var userIcon = MapSettings.getIcon(map.settings());
            if (userIcon != null) iconButton.model(userIcon.toString(), null);
            else iconButton.sprite("icon2/1_1/item_frame", 1, 1);

            add(1, 0, new Text(8, 1, mapName)
                .align(2, Text.CENTER)
                .translationKey(translationKey, mapName)
                .onLeftClick(this::viewMapDetails));
        }
    }
}
