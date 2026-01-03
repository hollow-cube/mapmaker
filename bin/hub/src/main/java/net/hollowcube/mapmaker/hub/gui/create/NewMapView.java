package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.MapSlot;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Select;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;

import java.time.Instant;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class NewMapView extends Panel {

    private final MapService mapService;
    private final Consumer<MapSlot> onNewMap;

    private final Select<MapSize> sizeSelect;

    public NewMapView(MapService mapService, Consumer<MapSlot> onNewMap) {
        super(9, 10);
        this.mapService = mapService;
        this.onNewMap = onNewMap;

        background("create_maps2/new/container", -10, -31);
        add(0, 0, title("Create New Map"));

        add(0, 0, backOrClose());

        sizeSelect = add(1, 2, new Select<>(4, MapSize.NORMAL));
        sizeSelect.addOption(MapSize.NORMAL, "normal", "create_maps2/size/1", 4, 5);
        sizeSelect.addOption(MapSize.LARGE, "large", "create_maps2/size/2", 3, 4);
        sizeSelect.addOption(MapSize.MASSIVE, "massive", "create_maps2/size/3", 3, 3);
        sizeSelect.addOption(MapSize.COLOSSAL, "colossal", "create_maps2/size/4", 2, 2);

        add(2, 4, new Text("create", 5, 1, "Create")
            .align(Text.CENTER, Text.CENTER)
            .background("generic2/btn/success/5_1")
            .onLeftClick(this::handleSubmit));
    }

    private void handleSubmit() {
        var playerId = PlayerData.fromPlayer(host.player()).id();
        var map = mapService.createMap(MapCreateRequest.forPlayerV2(
            playerId, sizeSelect.selected(),
            ProtocolVersions.getProtocolVersion(playerId)));
        onNewMap.accept(new MapSlot(map, Instant.now(), -1));
        host.popView();
    }
}
