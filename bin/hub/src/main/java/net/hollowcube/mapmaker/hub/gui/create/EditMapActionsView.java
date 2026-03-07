package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;

import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class EditMapActionsView extends Panel {

    private final ApiClient api;
    private final PlayerService playerService;
    private final MapService mapService;
    private final ServerBridge bridge;

    private final String mapId;

    public EditMapActionsView(ApiClient api, PlayerService playerService, MapService mapService, ServerBridge bridge, String mapId) {
        super(9, 10);
        this.api = api;
        this.playerService = playerService;
        this.mapService = mapService;
        this.bridge = bridge;
        this.mapId = mapId;

        background("create_maps2/edit/actions_container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(0, 0, backOrClose());

        add(1, 2, new Button("gui.create_maps.actions.copy_map", 3, 2));

        add(5, 2, new Button("gui.create_maps.actions.resize_map", 3, 2));

        add(1, 4, new Button("gui.create_maps.actions.transfer_ownership", 3, 2));

        add(5, 4, new Button("gui.create_maps.actions.delete_map", 3, 2)
            .onLeftClick(this::beginDeleteMap));
    }

    private void beginDeleteMap() {
        host.pushTransientView(ExtraPanels.confirm(null, this::confirmDeleteMap));
    }

    private void confirmDeleteMap() {
        final var player = Objects.requireNonNull(host.player());
        async(() -> {
            try {
                var playerId = PlayerData.fromPlayer(player).id();
                mapService.deleteMap(playerId, mapId, null);

                player.sendMessage(Component.translatable("command.map.delete.success"));
                sync(() -> this.host.replaceView(new CreateMapsView(this.api, this.playerService, this.mapService, this.bridge)));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                player.sendMessage(Component.translatable("command.map.delete.failure"));
                player.closeInventory();
            }
        });
    }
}
