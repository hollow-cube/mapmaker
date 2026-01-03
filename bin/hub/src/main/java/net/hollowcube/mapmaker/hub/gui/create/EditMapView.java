package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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

    private final MapData map;

    private final Text nameText;
    private final Button iconButton;

    private final Button verifyPublishButton;

    public EditMapView(ServerBridge bridge, MapData map) {
        super(9, 10);
        this.map = map;

        background("create_maps2/edit/container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(0, 0, backOrClose());
        this.nameText = add(1, 0, new Text("todo", 7, 1, map.settings().getNameSafe())
            .align(8, 5));
        this.nameText.onLeftClick(this::beginNameEdit);
        add(8, 0, new Button("more", 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/ellipsis", 1, 1)
            .onLeftClick(() -> host.pushView(new EditMapActionsView())));

        this.iconButton = add(1, 2, new Button("todo", 1, 1)
            .onLeftClick(this::beginIconEdit));
        updateIcon();

        add(3, 2, new Button("self", 1, 1)
            .background("create_maps2/head_outline", 4, 4)
            .model(MODEL_8X, null)
            .profile(getPlayerHead2d(map.owner())));
        for (int i = 0; i < 4; i++) {
            add(i + 4, 2, new Button("locked", 1, 1)
                .sprite("icon2/1_1/lock", 1, 1));
        }

        add(1, 4, new EditableMapTagList(map));

        add(1, 6, new Button("build", 3, 3)
            .background("create_maps2/edit/build")
            .onLeftClickAsync(() -> beginBuildingMap(bridge, map, host.player())));
        this.verifyPublishButton = add(5, 6, new Button("verify", 3, 3)
            .background("create_maps2/edit/verify_orange"));
    }

    private void beginNameEdit() {
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "icon2/anvil/planet",
            LanguageProviderV2.translateToPlain("setnametodo"),
            message -> {
                //TODO make this only update the display of the name in the GUI, appending ... to the end, and not messing with the actual name
                String limitedName = message.length() > MapData.MAX_NAME_LENGTH
                    ? message.substring(0, MapData.MAX_NAME_LENGTH) : message;

                map.settings().setName(limitedName);
                nameText.text(limitedName);
                //todo update verify button
            },
            map.settings().getName()
        ));
    }

    private void beginIconEdit() {
        host.pushView(new AnvilSearchView<>(
            "icon2/anvil/item_frame",
            LanguageProviderV2.translateToPlain("seticontodo"),
            (query, limit) -> {
                // If input is empty return some random results
                if (query.isEmpty()) {
                    //noinspection NullableProblems
                    return ThreadLocalRandom.current().ints(1, Material.values().size())
                        .mapToObj(Material::fromId)
                        .filter(ICON_SEARCH_PREDICATE)
                        .limit(limit)
                        .toList();
                }
                return Autocompletors.searchMaterials(query, limit, ICON_SEARCH_PREDICATE);
            },
            "",
            (icon) -> new Button(null, 1, 1)
                .text(LanguageProviderV2.getVanillaTranslation(icon)
                    .decoration(TextDecoration.ITALIC, false), List.of())
                .model(icon.key().asString(), null),
            (icon) -> {
                map.settings().setIcon(icon);
                updateIcon();
                //todo update verify button
            }
        ));
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

    @Blocking
    static void beginBuildingMap(ServerBridge bridge, MapData map, Player player) {
        try {
            // TODO: need to prompt for verification
//            if (map.verification() != MapVerification.UNVERIFIED) {
//                player.sendMessage(Component.translatable("progress.verification.lost"));
//
//                var playerData = PlayerData.fromPlayer(player);
//                mapService.deleteVerification(playerData.id(), map.id());
//            }

            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING, "edit_maps_gui");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("edit.map.failure"));
            ExceptionReporter.reportException(e, player);
            player.closeInventory();
        }
    }
}
