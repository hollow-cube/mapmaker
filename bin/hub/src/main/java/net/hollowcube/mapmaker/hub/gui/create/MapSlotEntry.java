package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapSlot;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;

import static net.hollowcube.mapmaker.hub.gui.create.EditMapView.beginBuildingMap;

public class MapSlotEntry extends Panel {

    private final ServerBridge bridge;
    private final MapSlot slot;

    public MapSlotEntry(ServerBridge bridge, MapSlot slot) {
        super(9, 1);
        this.bridge = bridge;
        this.slot = slot;

        background("create_maps2/slot/blue", 1, 1);

        var iconButton = add(0, 0, new Button("todo", 1, 1)
            .onLeftClick(this::onEditMap));
        var userIcon = slot.map().settings().getIcon();
        if (userIcon != null) {
            iconButton.model(userIcon.toString(), null);
        } else {
            iconButton.sprite("icon2/1_1/item_frame", 1, 1);
        }

        var name = slot.map().settings().getNameSafe();
        add(1, 0, new Text("todo", 7, 1, name)
            .align(2, Text.CENTER)
            .onLeftClick(this::onEditMap));

        add(8, 0, new Button("edit", 1, 1)
            .sprite("icon2/1_1/hammer", 1, 1)
            .onLeftClickAsync(() -> beginBuildingMap(bridge, slot.map(), host.player())));
    }

    private void onEditMap() {
        host.pushView(new EditMapView(bridge, slot.map()));
    }

}
