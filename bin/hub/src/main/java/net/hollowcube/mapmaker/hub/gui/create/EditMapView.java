package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.common.ConfirmActionView;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.AnvilSearchView;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.TabCompleteResponse;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
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

    private final PlayerService playerService;
    private final MapService mapService;

    private final MapData map;
    private final MapPublisher publisher;

    private final Text nameText;
    private final Button iconButton;

    @Blocking
    public EditMapView(PlayerService playerService, MapService mapService,
                       ServerBridge bridge, MapData map, Runnable onPublish) {
        super(9, 10);
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
        this.nameText = add(1, 0, new Text("gui.create_maps.edit.name", 7, 1, map.settings().getNameSafe())
            .align(8, 5));
        this.nameText.onLeftClick(this::beginNameEdit);
        add(8, 0, new Button("gui.create_maps.edit.actions", 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/ellipsis", 1, 1)
            .onLeftClick(() -> host.pushView(new EditMapActionsView(playerService, mapService, bridge, map.id()))));

        this.iconButton = add(1, 2, new Button("gui.create_maps.edit.icon", 1, 1)
            .onLeftClick(this::beginIconEdit));
        updateIcon();

        add(3, 2, new Button("gui.create_maps.edit.builders.owner.self", 1, 1)
            .background("create_maps2/head_outline", 4, 4)
            .model(MODEL_8X, null)
            .profile(getPlayerHead2d(map.owner())));
        for (int i = 0; i < 4; i++) {
            add(i + 4, 2, new Button("gui.create_maps.edit.builders.locked", 1, 1)
                .sprite("icon2/1_1/lock", 1, 1)
                .onLeftClick(this::beginAddMapBuilder));
//                // TODO: Remove before releasing
//                .onLeftClickAsync(() -> {
//                    mapService.inviteMapBuilder(map.id(), "d79d790a-8e90-4d78-958a-780c7fadeaab");
//                }));
        }

        // async doesn't work as host is null when this is called
        add(1, 4, new EditableMapTagList(map, this::updatePublishStage));

        add(1, 6, new Button("gui.create_maps.edit.build", 3, 3)
            .background("create_maps2/edit/build")
            .onLeftClickAsync(() -> editMap(map, this.host, bridge)));

        add(5, 6, this.publisher.getButton());
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

    private void beginAddMapBuilder() {
        var view = AnvilSearchView.<TabCompleteResponse.Entry>builder()
            // TODO: Make this the hammer icon
            .icon("icon2/anvil/item_frame")
            .title(LanguageProviderV2.translateToPlain("Add Map Builder"))
            .searchFunction(this::getTabCompletionsNotOwner)
            .defaultSearchTerm(".*")
            .buttonFactory(icon -> new Button(null, 1, 1)
                .text(this.getPlayerDisplayNameItemName(icon.id()), List.of())
                .model(MODEL_8X, null)
                .profile(getPlayerHead2d(icon.id())))
            .onSubmit(icon -> this.mapService.inviteMapBuilder(this.map.id(), icon.id()))
            .async()
            .build();
        this.host.pushView(view);
    }

    private Component getPlayerDisplayNameItemName(String uuid) {
        return this.playerService.getPlayerDisplayName2(uuid)
            .asComponent()
            .style(style -> style.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
    }

    private List<TabCompleteResponse.Entry> getTabCompletionsNotOwner(String query, int limit) {
        var results = this.playerService.getUsernameTabCompletions(query, limit).result();
        results.removeIf(result -> result.id().equals(this.map.owner()));
        return results;
    }

    @Blocking
    static void editMap(MapData map, InventoryHost host, ServerBridge bridge) {
        if (map.verification() != MapVerification.UNVERIFIED) {
            host.pushView(new ConfirmActionView(() -> beginBuildingMap(bridge, map, host.player()),
                                                Component.translatable("edit.map.confirm")));
        } else {
            beginBuildingMap(bridge, map, host.player());
        }
    }

    @Blocking
    private static void beginBuildingMap(ServerBridge bridge, MapData map, Player player) {
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
