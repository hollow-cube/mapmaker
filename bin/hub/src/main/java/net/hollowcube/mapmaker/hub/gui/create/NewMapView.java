package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.panels.buttons.LockedButton;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;

import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.panels.RadioSelect.ButtonUpdater.SQUARE_BACKGROUND_EX;

public class NewMapView extends Panel {

    private final MapClient maps;
    private final PlayerService playerService;
    private final Consumer<MapData> onNewMap;

    private final RadioSelect<MapSize> sizeSelect;
    private final Button confirmButton;

    public NewMapView(MapClient maps, PlayerService playerService, Consumer<MapData> onNewMap) {
        super(9, 10);
        this.maps = maps;
        this.playerService = playerService;
        this.onNewMap = onNewMap;

        background("create_maps2/new/container", -10, -31);
        add(0, 0, title(LanguageProviderV2.translateToPlain("gui.create_maps.new.name")));

        add(0, 0, backOrClose());

        add(1, 1, infoText(4, "normal sizes"));
        sizeSelect = add(1, 2, new RadioSelect<>(4, 1, MapSize.NORMAL))
            .onChange(this::updateConfirmButton);

        confirmButton = add(2, 4, new Text(5, 1, "Create")
            .align(Text.CENTER, Text.CENTER)
            .background("generic2/btn/success/5_1")
            .lorePostfix(LORE_POSTFIX_CLICKCREATE)
            .onLeftClickAsync(this::handleSubmit));

        updateConfirmButton(sizeSelect.selected()); // Update to initial
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        for (var mapSize : MapSize.GUI_SIZES) {
            boolean locked = isLocked(mapSize);
            Button.Constructor makeButton = (tk, slotWidth, slotHeight) -> {
                var button = locked
                    ? new LockedButton(tk, slotWidth, slotHeight)
                    : new Button(tk, slotWidth, slotHeight);
                return button.sprite("icon2/1_1/" + mapSize.icon(), 1, 1);
            };

            RadioSelect.ButtonUpdater updateButton = (button, selected) -> {
                SQUARE_BACKGROUND_EX.update(button, selected);
                if (locked) {
                    button.translationKey("gui.create_maps.new.size." + mapSize.name().toLowerCase() + ".locked");
                } else {
                    button.translationKey("gui.create_maps.new.size." + mapSize.name().toLowerCase() + (selected ? ".on" : ".off"));
                }
            };

            if (locked) {
                var button = makeButton.construct(null, 1, 1)
                    .onLeftClick(this::handleOpenStore);
                updateButton.update(button, false);
                sizeSelect.add(sizeSelect.index++, 0, button);
            } else {
                sizeSelect.addOption(mapSize, updateButton, makeButton);
            }
        }
    }

    private void handleSubmit() {
        var playerId = PlayerData.fromPlayer(host.player()).id();
        var map = maps.create(playerId, sizeSelect.selected());
        sync(() -> {
            var host = this.host;
            onNewMap.accept(map);
            if (host.canPopView()) host.popView();
        });
    }

    private void handleOpenStore() {
        host.pushView(new StoreView(playerService, StoreView.TAB_ADDONS));
    }

    private void updateConfirmButton(MapSize size) {
        var sizeNameKey = "gui.create_maps.new.size." + size.name().toLowerCase() + ".on.name";
        var actualSizeKey = "gui.create_maps.new.size." + size.name().toLowerCase() + ".size";
        confirmButton.translationKey("gui.create_maps.new.confirm",
            Component.translatable(sizeNameKey),
            Component.translatable(actualSizeKey));
    }

    private boolean isLocked(MapSize size) {
        return !PlayerData.fromPlayer(this.host.player()).maxMapSize().unlocks(size);
    }
}
