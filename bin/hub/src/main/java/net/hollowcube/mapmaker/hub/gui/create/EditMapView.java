package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.components.ExtraComponents;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.ipc.map.DeleteVerificationResult;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.ipc.map.MapSlot;
import net.hollowcube.ipc.map.MapVerification;
import net.hollowcube.ipc.player.PlayerStub;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.maps.MapWriteMessages;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.gui.store.StoreHelpers;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.LocalPlayer;
import net.hollowcube.mapmaker.player.AccountService;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.hollowcube.mapmaker.util.Sanity;
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

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.MODEL_8X;
import static net.hollowcube.mapmaker.gui.map.details.MapDetailsTimesPanel.getPlayerHead2d;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;
import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class EditMapView extends Panel {
    private static final Predicate<@Nullable Material> ICON_SEARCH_PREDICATE = material ->
        material != null &&
        material != Material.AIR &&
        material != Material.SCULK_SENSOR &&
        material != Material.CALIBRATED_SCULK_SENSOR &&
        material != Material.RECOVERY_COMPASS &&
        !material.name().endsWith("glass_pane");

    private static final int NAME_INPUT_MAX = 100;

    private final ApiClient api;
    private final AccountService accountService;

    private MapSlot slot;
    private final MapPatch.Builder editor;
    private final Consumer<MapSlot> onEdit;
    private final MapPublisher publisher;

    private final Text nameText;
    private final Button iconButton;

    private final Panel builderButtons;

    @Blocking
    public EditMapView(
        ApiClient api,
        AccountService accountService,
        ServerBridge bridge, MapSlot slot,
        Runnable onPublish, Consumer<MapSlot> onEdit
    ) {
        super(9, 10);
        this.api = api;
        this.accountService = accountService;
        this.slot = slot;
        this.editor = new MapPatch.Builder(slot.map());
        this.onEdit = onEdit;

        Consumer<MapData> publishCallback = publishedMap -> {
            this.host.replaceView(new MapDetailsView(api, bridge, publishedMap, true));
            onPublish.run();
        };
        this.publisher = new MapPublisher(api, bridge, editor, () -> this.host, publishCallback);

        background("create_maps2/edit/container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(0, 0, backOrClose());
        var name = FontUtil.shorten(MapSettings.getNameSafe(editor.map().settings()), NAME_INPUT_MAX, 5);
        this.nameText = add(1, 0, new Text("gui.create_maps.edit.name", 7, 1, name).align(8, 5));
        this.nameText.lorePostfix(LORE_POSTFIX_CLICKEDIT)
            .onLeftClick(this::beginNameEdit);
        add(8, 0, new Button("gui.create_maps.edit.actions", 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/ellipsis", 1, 1)
            .onLeftClick(() -> host.pushView(new EditMapActionsView(api.maps, editor.map()))));

        add(1, 1, infoText(1, "icon", -2));
        this.iconButton = add(1, 2, new Button("gui.create_maps.edit.icon", 1, 1)
            .lorePostfix(LORE_POSTFIX_CLICKCHOOSE)
            .onLeftClick(this::beginIconEdit));
        updateIcon();

        add(3, 1, infoText(5, "builders", -2));
        add(3, 2, new Button("gui.create_maps.edit.builders.owner.self", 1, 1)
            .background("create_maps2/head_outline", 4, 4)
            .model(MODEL_8X, null)
            .profile(getPlayerHead2d(editor.map().owner())));
        this.builderButtons = add(4, 2, new Panel(4, 1) {});

        // async doesn't work as host is null when this is called
        add(1, 3, infoText(1, "tags", -2));
        add(1, 4, new EditableMapTagList(editor, this::updatePublishStage));

        add(1, 6, new Button("gui.create_maps.edit.build", 3, 3)
            .background("create_maps2/edit/build")
            .onLeftClickAsync(() -> editMap(api.maps, editor.map(), this.host, bridge)));

        add(5, 6, this.publisher.getButton());
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        drawBuilderButtons();
    }

    private void drawBuilderButtons() {
        var pd = localPlayer(host.player());
        // When we want to show a read-only view we will need to change this, for now not necessary.
        Sanity.check(pd.id().equals(editor.map().owner()), "can only view your own maps right now");

        var builderSlots = pd.mapBuilders();

        builderButtons.clear();
        boolean addedAddButton = false;
        for (int i = 0; i < 4; i++) {
            Button button;
            if (i < slot.builders().size()) {
                // If there is a builder, always show them (even if over unlocked count)
                var builder = slot.builders().get(i);

                button = new Button(null, 1, 1)
                    .background("create_maps2/head_outline" + (builder.pending() ? "_pending" : ""), 4, 4)
                    .model(MODEL_8X, null)
                    .profile(getPlayerHead2d(builder.id()));

                async(() -> {
                    var displayName = api.players.displayName(builder.id());
                    button.translationKey("gui.create_maps.edit.builders." + (builder.pending() ? "pending" : "entry"), displayName.render())
                        .onRightClick(() -> host.pushView(ExtraPanels.confirm(
                            "Remove " + displayName.username() + "?",
                            () -> FutureUtil.submitVirtual(() -> removeMapBuilder(builder.id().toString())))));
                });
            } else if (i < builderSlots) {
                if (addedAddButton) continue;
                addedAddButton = true;

                button = new Button("gui.create_maps.edit.builders.add", 1, 1)
                    .sprite("icon2/1_1/plus", 1, 1)
                    .onLeftClick(this::beginAddMapBuilder);
            } else {
                boolean hasCubits = pd.cubits() >= ShopUpgrade.MAP_BUILDER_2.cubits();
                String cubitsKey = hasCubits ? "has_cubits" : "no_cubits";

                button = new Button("gui.create_maps.edit.builders.locked." + cubitsKey, 1, 1)
                    .sprite("icon2/1_1/lock", 1, 1)
                    .onLeftClick(this::buyBuilderPrimary)
                    .onRightClick(this::buyBuilderSecondary);
            }

            builderButtons.add(i, 0, button);
        }
    }

    @Override
    protected void unmount() {
        onEdit.accept(new MapSlot(editor.map(), slot.createdAt(), slot.owner(), slot.builders()));
        // Updating on unmount is kinda unnecessary since itll happen when opening the add tag
        // menu for example. But at time of writing we dont have a "when really gone" callback.
        final var player = host.player();
        async(() -> editor.save(req -> {
            try {
                api.maps.update(editor.map().id().toString(), req);
                return editor.map();
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                return null;
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

                editor.setName(limitedName);

                var name = FontUtil.shorten(MapSettings.getNameSafe(editor.map().settings()), NAME_INPUT_MAX, 5);
                nameText.text(name);
                updatePublishStage();
            },
            editor.map().settings().name()
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
                editor.setIcon(icon.name());
                updateIcon();
                updatePublishStage();
                return true;
            })
            .build();
        host.pushView(view);
    }

    private void updateIcon() {
        var userIcon = MapSettings.getIcon(editor.map().settings());
        if (userIcon != null) {
            iconButton.sprite((Sprite) null);
            iconButton.model(userIcon.toString(), null);
        } else {
            iconButton.model("minecraft:air", null);
            iconButton.sprite("icon2/1_1/plus", 1, 1);
        }
    }

    private void buyBuilderPrimary() {
        var playerData = localPlayer(host.player());
        boolean hasCubits = playerData.cubits() >= ShopUpgrade.MAP_BUILDER_2.cubits();

        if (hasCubits) {
            host.pushView(confirm("Buy Trusted Builder", FutureUtil.virtual(this::buyBuilderSlot)));
        } else {
            this.host.pushView(new StoreView(accountService, StoreView.TAB_HYPERCUBE));
        }
    }

    private void buyBuilderSecondary() {
        var playerData = localPlayer(host.player());
        boolean hasCubits = playerData.cubits() >= ShopUpgrade.MAP_BUILDER_2.cubits();

        this.host.pushView(new StoreView(
            accountService,
            hasCubits ? StoreView.TAB_HYPERCUBE : StoreView.TAB_CUBITS
        ));
    }

    @Blocking
    private void buyBuilderSlot(Player player) {
        var playerData = localPlayer(player);
        var nextSlot = latestBuilderUpgrade(playerData);
        if (nextSlot == null) return;

        StoreHelpers.buyUpgrade(accountService, player, nextSlot);
        sync(this::drawBuilderButtons);
    }

    private static @Nullable ShopUpgrade latestBuilderUpgrade(LocalPlayer playerData) {
        for (var upgrade : ShopUpgrade.MAP_BUILDERS) {
            if (!upgrade.has(playerData)) return upgrade;
        }
        return null;
    }

    private void beginAddMapBuilder() {
        host.pushView(AnvilSearchView.<PlayerStub>builder()
            .icon("icon2/anvil/construction_hat")
            .title("Add Map Builder")
            .searchFunction((query, limit) -> api.players.search(query, List.of(editor.map().owner()), limit))
            // todo would be cool to default to some online players
            // adding onto the above, probably their online friends
            .defaultSearchTerm("")
            // TODO if the player is already invited they should not be clickable
            .buttonFactory(pds -> {
                var button = new Button(null, 1, 1)
                    .text(ExtraComponents.noItalic(pds.displayName().render()), List.of())
                    .model(MODEL_8X, null)
                    .profile(getPlayerHead2d(pds.id()));
                if (isPlayerInvitePending(pds.id().toString())) {
//                    button.background("create_maps2/head_outline_pending", 4, 4)
//                        .translationKey("gui.create_maps.edit.builders.already_invited", pds.displayName().asComponent());
                    button.lorePostfix(List.of(Component.translatable("gui.create_maps.edit.builders.add.search.entry.already_invited.lore")));
                } else if (isPlayerInvited(pds.id().toString())) {
                    button.lorePostfix(List.of(Component.translatable("gui.create_maps.edit.builders.add.search.entry.already_added.lore")));
                } else if (!PlayerSettings.ALLOW_BUILDER_INVITES.read(pds.settings())) {
                    button.lorePostfix(List.of(Component.translatable("gui.create_maps.edit.builders.add.search.entry.invites_disabled.lore")));
                }
                return button;
            })
            .onSubmit(this::addMapBuilder)
            .async()
            .build());
    }

    @Blocking
    private boolean addMapBuilder(PlayerStub pds) {
        if (isPlayerInvited(pds.id().toString()) || !PlayerSettings.ALLOW_BUILDER_INVITES.read(pds.settings()))
            return false;

        var result = api.maps.inviteMapBuilder(editor.map().id().toString(), pds.id().toString());
        if (!result.succeeded()) {
            host.player().sendMessage(MapWriteMessages.failure(result));
            return false;
        }
        slot = new MapSlot(editor.map(), slot.createdAt(), slot.owner(), result.builders());
        return true;
    }

    @Blocking
    private void removeMapBuilder(String builderId) {
        var result = api.maps.removeMapBuilder(editor.map().id().toString(), builderId);
        if (!result.succeeded()) {
            host.player().sendMessage(MapWriteMessages.failure(result));
            return;
        }
        sync(() -> {
            slot = new MapSlot(editor.map(), slot.createdAt(), slot.owner(), result.builders());
            drawBuilderButtons();
        });
    }

    @Blocking
    static void editMap(MapClient maps, MapData map, InventoryHost host, ServerBridge bridge) {
        if (map.verification() != MapVerification.UNVERIFIED) {
            host.pushView(ExtraPanels.confirm("Reset Verification Progress?",
                FutureUtil.wrapVirtual(() -> buildMapAfterVerify(maps, bridge, map, host.player()))));
        } else {
            beginBuildingMap(bridge, map, host.player());
        }
    }

    @Blocking
    private static void buildMapAfterVerify(MapClient maps, ServerBridge bridge, MapData map, Player player) {
        var result = maps.deleteVerification(map.id().toString());
        if (result != DeleteVerificationResult.RESET) {
            player.sendMessage(MapWriteMessages.verification(result));
            return;
        }
        player.sendMessage(Component.translatable("progress.verification.lost"));
        beginBuildingMap(bridge, map, player);
    }

    @Blocking
    static void beginBuildingMap(ServerBridge bridge, MapData map, Player player) {
        try {
            player.closeInventory();
            bridge.joinMap(player, map.id().toString(), ServerBridge.JoinMapState.EDITING, "edit_maps_gui");
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

    private boolean isPlayerInvited(String playerId) {
        return slot.builders().stream().anyMatch(builder -> builder.id().toString().equals(playerId));
    }

    private boolean isPlayerInvitePending(String playerId) {
        return slot.builders().stream().anyMatch(builder -> builder.id().toString().equals(playerId) && builder.pending());
    }
}
