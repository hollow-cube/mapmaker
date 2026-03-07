package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class NewMapView extends Panel {

    private final MapService mapService;
    private final Consumer<MapData> onNewMap;

    private final LockableRadioSelect<MapSize> sizeSelect;
    private final Button confirmButton;

    public NewMapView(MapService mapService, Consumer<MapData> onNewMap) {
        super(9, 10);
        this.mapService = mapService;
        this.onNewMap = onNewMap;

        background("create_maps2/new/container", -10, -31);
        add(0, 0, title(LanguageProviderV2.translateToPlain("gui.create_maps.new.name")));

        add(0, 0, backOrClose());

        sizeSelect = add(1, 2, new LockableRadioSelect<>(4, 1, MapSize.NORMAL, this::isLocked));

        confirmButton = add(2, 4, new Text(5, 1, "Create")
            .align(Text.CENTER, Text.CENTER)
            .background("generic2/btn/success/5_1")
            .onLeftClickAsync(this::handleSubmit));
        updateConfirmButton();
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        // Init on mount so we know the host is bound for isLocked
        sizeSelect.addLockableOption(MapSize.NORMAL, "gui.create_maps.new.size.normal", "icon2/1_1/house_1", 1, 1);
        sizeSelect.addLockableOption(MapSize.LARGE, "gui.create_maps.new.size.large", "icon2/1_1/house_2", 1, 1);
        sizeSelect.addLockableOption(MapSize.MASSIVE, "gui.create_maps.new.size.massive", "icon2/1_1/house_3", 1, 1);
        sizeSelect.addLockedOption("gui.create_maps.new.size.colossal.on", "icon2/1_1/castle", 1, 1);
        sizeSelect.onChange(this::updateConfirmButton);
    }

    private void handleSubmit() {
        var playerId = PlayerData.fromPlayer(host.player()).id();
        var map = mapService.createMap(MapCreateRequest.forPlayerV2(
            playerId, sizeSelect.selected(),
            ProtocolVersions.getProtocolVersion(playerId)));
        sync(() -> {
            onNewMap.accept(map);
            host.popView();
        });
    }

    private void updateConfirmButton() {
        var size = sizeSelect.selected().name().toLowerCase(Locale.ROOT);
        var sizeNameKey = "gui.create_maps.new.size." + size + ".on.name";
        var actualSizeKey = "gui.create_maps.new.size." + size + ".size";
        confirmButton.translationKey("gui.create_maps.new.confirm",
            Component.translatable(sizeNameKey),
            Component.translatable(actualSizeKey));
    }

    private boolean isLocked(MapSize size) {
        return !PlayerData.fromPlayer(this.host.player()).maxMapSize().unlocks(size);
    }
}
