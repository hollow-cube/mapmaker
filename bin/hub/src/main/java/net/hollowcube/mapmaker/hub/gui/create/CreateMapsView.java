package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSlot;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class CreateMapsView extends Panel {
    public static final List<Component> LORE_POSTFIX_CLICKSELECT = LanguageProviderV2.translateMulti("gui.action.clickselect", List.of());

    private final MapService mapService;
    private final ServerBridge bridge;

    private final Button createButton;
    private final Panel entryContainer;

    private final List<MapSlot> slots = new ArrayList<>();
    private int page = 0;

    public CreateMapsView(MapService mapService, ServerBridge bridge) {
        super(9, 10);
        this.mapService = mapService;
        this.bridge = bridge;

        background("create_maps2/container", -10, -31);
        add(0, 0, title("Create Map"));

        add(0, 0, backOrClose());
        // todo search
        this.createButton = add(8, 0, new Button(1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("icon2/1_1/plus", 1, 1)
            .onLeftClick(() -> host.pushTransientView(new NewMapView(mapService, this::acceptNewMap))));

        this.entryContainer = add(0, 1, new Panel(9, 5) {});
        update();
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (isInitial) {
            // full load from server
            async(() -> {
                var playerId = PlayerData.fromPlayer(host.player()).id();
                var remoteSlots = mapService.getPlayerMapSlots(playerId);

                sync(() -> {
                    this.updateCreateButton();

                    slots.clear();
                    slots.addAll(remoteSlots);
                    slots.sort((MapSlot a, MapSlot b) -> b.createdAt().compareTo(a.createdAt()));
                    update();
                });
            });
        } else {
            // Update from changes (name/icon generally)
            update();
        }
    }

    private void acceptNewMap(MapSlot slot) {
        // No need to re-sort, we know this should be first in the list.
        slots.addFirst(slot);
        update();
    }

    private void update() {
        this.entryContainer.clear();
        for (int i = 0; i < 5; i++) {
            int index = i + page * 5;
            if (index >= slots.size()) break;
            this.entryContainer.add(0, i, new MapSlotEntry(mapService, bridge, slots.get(index)));
        }
    }

    private void updateCreateButton() {
        // We have to lazy init this as host is not set until the view is mounted
        var data = MapPlayerData.fromPlayer(this.host.player());
        var unusedSlots = data.unlockedSlots() - this.slots.size();
        this.createButton.translationKey("gui.create_maps.new", unusedSlots);
    }
}
