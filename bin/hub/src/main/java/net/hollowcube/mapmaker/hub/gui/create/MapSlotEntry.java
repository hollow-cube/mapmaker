package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapSlot;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.Sanity;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.confirm;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;

public class MapSlotEntry extends Panel {

    private final ApiClient api;
    private final PlayerService playerService;
    private final ServerBridge bridge;
    private final MapSlot slot;
    private final Runnable onPublish;

    private MapSlotEntry(
        ApiClient api,
        PlayerService playerService, ServerBridge bridge,
        MapSlot slot, Runnable onPublish
    ) {
        super(9, 1);
        this.api = api;
        this.playerService = playerService;
        this.bridge = bridge;
        this.slot = slot;
        this.onPublish = onPublish;
    }

    @Blocking
    protected void buildInWorld() {
        if (isOwner(host.player())) {
            EditMapView.editMap(api.maps, slot.map(), this.host, bridge);
            return;
        }

        // If you arent the owner, we need to check the latest version to make sure its not in a verifying state
        // TODO: this is still a race, we need to check elsewhere to prevent editing a map during/after verification
        var map = api.maps.get(slot.map().id());
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
            var view = new EditMapView(this.api, this.playerService, this.bridge, slot, this.onPublish);
            sync(() -> host.pushView(view));
        });
    }

    protected void viewMapDetails() {
        // We only show this for the player themselves anyway so its fine to just get it from them.
        Sanity.check(isOwner(host.player()), "cannot show details for someone else's map");

        var displayName = PlayerData.fromPlayer(host.player()).displayName2();
        var view = new MapDetailsView(api, bridge, slot.map(), displayName, true);
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
            ApiClient api,
            PlayerService playerService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, playerService, bridge, slot, onPublish);

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
        private static final String TRANSLATION_KEY = "gui.create_maps.slot.other";
        private static final Component LOADING = Component.text("loading...");

        private final ApiClient api;
        private final MapSlot slot;

        private final Button iconButton;
        private final Button nameButton;

        public Builder(
            ApiClient api,
            PlayerService playerService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, playerService, bridge, slot, onPublish);
            this.api = api;
            this.slot = slot;

            var map = slot.map();
            var mapName = map.settings().getNameSafe();

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
                var ownerDisplayName = api.players.getDisplayName(slot.map().owner()).asComponent();
                sync(() -> {
                    var mapName = slot.map().settings().getNameSafe();

                    this.iconButton.translationKey(TRANSLATION_KEY, mapName, ownerDisplayName);
                    this.nameButton.translationKey(TRANSLATION_KEY, mapName, ownerDisplayName);
                });
            });


        }
    }

    public static final class Published extends MapSlotEntry {
        public Published(
            ApiClient api,
            PlayerService playerService, ServerBridge bridge,
            MapSlot slot, Runnable onPublish
        ) {
            super(api, playerService, bridge, slot, onPublish);

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
