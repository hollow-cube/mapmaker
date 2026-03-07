package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.common.ConfirmActionView;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.TabCompleteResponse;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class EditMapView extends Panel {
    private static final Predicate<@Nullable Material> ICON_SEARCH_PREDICATE = material ->
        material != null &&
        material != Material.AIR &&
        material != Material.SCULK_SENSOR &&
        material != Material.CALIBRATED_SCULK_SENSOR &&
        material != Material.RECOVERY_COMPASS &&
        !material.name().endsWith("glass_pane");

    private final ApiClient api;
    private final PlayerService playerService;
    private final MapService mapService;

    private final MapData map;
    private final MapPublisher publisher;

    private final Text nameText;
    private final Button iconButton;

    private final Button[] mapBuilderButtons = new Button[4];

    @Blocking
    public EditMapView(ApiClient api, PlayerService playerService, MapService mapService,
                       ServerBridge bridge, MapData map, Runnable onPublish) {
        super(9, 10);
        this.api = api;
        this.playerService = playerService;
        this.mapService = mapService;
        this.map = map;

        Consumer<MapData> publishCallback = publishedMap -> {
            this.onMapPublish(playerService, mapService, bridge, publishedMap);
            onPublish.run();
        };
        this.publisher = new MapPublisher(mapService, bridge, map, () -> this.host, publishCallback);

        background("create_maps2/edit/container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(0, 0, backOrClose());
        this.nameText = add(1, 0, new Text("gui.create_maps.edit.name", 7, 1, map.settings().getNameSafe()).align(8, 5));
        this.nameText.onLeftClick(this::beginNameEdit);
        add(8, 0, new Button("gui.create_maps.edit.actions", 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/ellipsis", 1, 1)
            .onLeftClick(() -> host.pushView(new EditMapActionsView(api, playerService, mapService, bridge, map.id()))));

        this.iconButton = add(1, 2, new Button("gui.create_maps.edit.icon", 1, 1)
            .onLeftClick(this::beginIconEdit));
        updateIcon();

        add(3, 2, new Button("gui.create_maps.edit.builders.owner.self", 1, 1)
            .background("create_maps2/head_outline", 4, 4)
            .model(MODEL_8X, null)
            .profile(getPlayerHead2d(map.owner())));

        // async doesn't work as host is null when this is called
        add(1, 4, new EditableMapTagList(map, this::updatePublishStage));

        add(1, 6, new Button("gui.create_maps.edit.build", 3, 3)
            .background("create_maps2/edit/build")
            .onLeftClickAsync(() -> editMap(mapService, map, this.host, bridge)));

        add(5, 6, this.publisher.getButton());
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        FutureUtil.submitVirtual(this::initMapBuilderEntries);
    }

    @Blocking
    private void initMapBuilderEntries() {
        var mapBuilders = this.mapService.getMapBuilders(this.map.id());

        var builderSlots = this.playerService.getPlayerData(this.map.owner()).mapBuilders();
        // if this is ever false, something has gone horrifically wrong
        assert builderSlots >= mapBuilders.size();

        for (int i = 0; i < 4; i++) {
            Button button;
            if (i >= mapBuilders.size()) {
                button = this.createNoBuilderButton(i, builderSlots);
            } else {
                var builder = mapBuilders.get(i);
                button = this.createBuilderButton(i, builder.playerId(), builder.pending());
            }

            this.mapBuilderButtons[i] = add(i + 4, 2, button);
        }
    }

    private Button createNoBuilderButton(final int index, int builderSlots) {
        var button = new Button(null, 1, 1);
        if (index >= builderSlots) {
            return button.translationKey("gui.create_maps.edit.builders.locked").sprite("icon2/1_1/lock");
        } else {
            // an unlocked builder slot that has no assigned builder
            return button.translationKey("gui.create_maps.edit.builders.add")
                .sprite("icon2/1_1/plus", 1, 1)
                .onLeftClick(() -> this.beginAddMapBuilder(index));
        }
    }

    private Button createBuilderButton(int index, String builderId, boolean pending) {
        var displayName = this.playerService.getPlayerDisplayName2(builderId);
        return new Button(null, 1, 1)
            .background("create_maps2/head_outline" + (pending ? "_pending" : ""), 4, 4)
            .model(MODEL_8X, null)
            .profile(getPlayerHead2d(builderId))
            .translationKey("gui.create_maps.edit.builders." + (pending ? "pending" : "entry"), displayName.asComponent())
            .onRightClick(() -> {
                final var host = this.host;
                Runnable callback = () -> {
                    this.removeMapBuilder(host.player(), builderId, index);
                    sync(host::popView);
                };
                this.host.pushView(new ConfirmActionView(callback, Component.translatable("remove map builder")));
            });
    }

    private void replaceBuilderButton(int index, Button newButton) {
        this.mapBuilderButtons[index] = newButton;
        // TODO
//        replace(index + 4, 2, newButton);
    }

    @Override
    protected void unmount() {
        // Updating on unmount is kinda unnecessary since itll happen when opening the add tag
        // menu for example. But at time of writing we dont have a "when really gone" callback.
        final var player = host.player();
        async(() -> map.settings().withUpdateRequest(req -> {
            try {
                mapService.updateMap(player.getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                return false;
            }
        }));

        super.unmount();
    }

    private void beginNameEdit() {
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "icon2/anvil/planet",
            LanguageProviderV2.translateToPlain("Name Map"),
            message -> {
                //TODO make this only update the display of the name in the GUI, appending ... to the end, and not messing with the actual name
                String limitedName = message.length() > MapData.MAX_NAME_LENGTH
                    ? message.substring(0, MapData.MAX_NAME_LENGTH) : message;

                map.settings().setName(limitedName);
                nameText.text(limitedName);
                updatePublishStage();
            },
            map.settings().getName()
        ));
    }

    private void beginIconEdit() {
        var view = AnvilSearchView.<Material>builder()
            .icon("icon2/anvil/item_frame")
            .title(LanguageProviderV2.translateToPlain("Set Map Icon"))
            .searchFunction((query, limit) -> {
                // If input is empty return some random results
                if (query.isEmpty()) {
                    return ThreadLocalRandom.current().ints(1, Material.values().size())
                        .mapToObj(Material::fromId)
                        .filter(ICON_SEARCH_PREDICATE)
                        .limit(limit)
                        .toList();
                }
                return Autocompletors.searchMaterials(query, limit, ICON_SEARCH_PREDICATE);
            })
            .defaultSearchTerm("")
            .buttonFactory(icon -> new Button(null, 1, 1)
                .text(LanguageProviderV2.getVanillaTranslation(icon)
                    .decoration(TextDecoration.ITALIC, false), List.of())
                .model(icon.key().asString(), null))
            .onSubmit(icon -> {
                map.settings().setIcon(icon);
                updateIcon();
                updatePublishStage();
            })
            .build();
        host.pushView(view);
    }

    private void updateIcon() {
        var userIcon = map.settings().getIcon();
        if (userIcon != null) {
            iconButton.sprite((Sprite) null);
            iconButton.model(userIcon.toString(), null);
        } else {
            iconButton.model("minecraft:air", null);
            iconButton.sprite("icon2/1_1/plus", 1, 1);
        }
    }

    private void beginAddMapBuilder(int index) {
        // TODO
//        var view = AnvilSearchView.<TabCompleteResponse.Entry>builder()
//            .icon("icon2/1_1/hammer")
//            .title(LanguageProviderV2.translateToPlain("Add Map Builder"))
//            .searchFunction(this::getTabCompletionsNotOwner)
//            .defaultSearchTerm(".*")
//            .buttonFactory(icon -> new Button(null, 1, 1)
//                .text(ExtraComponents.noItalic(this.playerService.getPlayerDisplayName2(icon.id())), List.of())
//                .model(MODEL_8X, null)
//                .profile(getPlayerHead2d(icon.id())))
//            .onSubmitWithPlayer((icon, player) -> this.addMapBuilder(index, icon, player))
//            .async()
//            .build();
//        this.host.pushView(view);
    }

    @Blocking
    private void addMapBuilder(int index, TabCompleteResponse.Entry icon, Player player) {
        try {
            this.mapService.inviteMapBuilder(this.map.id(), icon.id());
            player.sendMessage(Component.text("map builder invited")); // todo
        } catch (MapService.AlreadyExistsError _) {
            player.sendMessage(Component.text("map builder already invited!")); // todo
        }

        this.replaceBuilderButton(index, this.createBuilderButton(index, icon.id(), true));
    }

    private List<TabCompleteResponse.Entry> getTabCompletionsNotOwner(String query, int limit) {
        var results = this.playerService.getUsernameTabCompletions(query, limit).result();
        results.removeIf(result -> this.map.owner().equals(result.id()));
        return results;
    }

    private void removeMapBuilder(Player player, String builderId, int index) {
        FutureUtil.submitVirtual(() -> this.mapService.removeMapBuilder(this.map.id(), builderId));

        var builderSlots = PlayerData.fromPlayer(player).mapBuilders();
        this.replaceBuilderButton(index, this.createNoBuilderButton(index, builderSlots));
    }

    @Blocking
    static void editMap(MapService mapService, MapData map, InventoryHost host, ServerBridge bridge) {
        if (map.verification() != MapVerification.UNVERIFIED) {
            host.pushView(new ConfirmActionView(
                () -> buildMapAfterVerify(mapService, bridge, map, host.player()),
                Component.translatable("edit.map.confirm")));
        } else {
            beginBuildingMap(bridge, map, host.player());
        }
    }

    private static void buildMapAfterVerify(MapService mapService, ServerBridge bridge, MapData map, Player player) {
        player.sendMessage(Component.translatable("progress.verification.lost"));

        var playerData = PlayerData.fromPlayer(player);
        mapService.deleteVerification(playerData.id(), map.id());

        beginBuildingMap(bridge, map, player);
    }

    @Blocking
    static void beginBuildingMap(ServerBridge bridge, MapData map, Player player) {
        try {
            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING, "edit_maps_gui");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("edit.map.failure"));
            ExceptionReporter.reportException(e, player);
            player.closeInventory();
        }
    }

    private void updatePublishStage() {
        // This is often called from contexts where host is null so cannot use async here
        FutureUtil.submitVirtual(this.publisher::updateStage);
    }

    private void onMapPublish(PlayerService playerService, MapService mapService, ServerBridge bridge, MapData publishedMap) {
        var authorName = playerService.getPlayerDisplayName2(publishedMap.owner());
        this.host.replaceView(new MapDetailsView(playerService, mapService, bridge, publishedMap, authorName, true));
    }
}
