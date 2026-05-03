package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;

import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class EditMapActionsView extends Panel {

    private final MapClient maps;

    private final MapData map;

    public EditMapActionsView(MapClient maps, MapData map) {
        super(9, 10);
        this.maps = maps;
        this.map = map;

        background("create_maps2/edit/actions_container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(0, 0, backOrClose());

        add(1, 1, new Text(null, 7, 1, "map actions")
            .font("small").align(Text.CENTER, Text.CENTER));

        add(1, 2, new Button("gui.create_maps.actions.copy_map", 3, 2));

        add(5, 2, new Button(3, 2).translationKey("gui.create_maps.actions.resize_map", map.settings().getSize()));

        add(1, 4, new Button("gui.create_maps.actions.transfer_ownership", 3, 2));

        add(5, 4, new Button("gui.create_maps.actions.delete_map", 3, 2)
            .onLeftClick(this::beginDeleteMap));
    }

    private void beginDeleteMap() {
        host.pushTransientView(ExtraPanels.confirm("Delete Map?", this::confirmDeleteMap));
    }

    private void confirmDeleteMap() {
        final var player = Objects.requireNonNull(host.player());
        async(() -> {
            try {
                var playerId = PlayerData.fromPlayer(player).id();
                maps.delete(playerId, map.id(), null);

                player.closeInventory();
                player.sendMessage(Component.translatable("command.map.delete.success"));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                player.sendMessage(Component.translatable("command.map.delete.failure"));
                player.closeInventory();
            }
        });
    }
}
