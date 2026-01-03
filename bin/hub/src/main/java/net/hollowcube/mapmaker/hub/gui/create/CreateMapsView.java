package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSlot;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class CreateMapsView extends Panel {

    private final List<MapSlot> slots = new ArrayList<>();

    public CreateMapsView(MapService mapService) {
        super(9, 10);

        background("create_maps2/container", -10, -31);
        add(0, 0, title("Create Map"));

        add(0, 0, backOrClose());
        // todo search
        // todo + to add map
        add(8, 0, new Button("create", 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("create_maps2/add", 4, 3)
            .onLeftClick(() -> host.pushTransientView(new NewMapView(mapService, this::acceptNewMap))));

        add(0, 1, new Button("todo", 9, 1)
            .background("create_maps2/slot/blue", 1, 1));
        add(0, 2, new Button("todo", 9, 1)
            .background("create_maps2/slot/blue", 1, 1));
        add(0, 3, new Button("todo", 9, 1)
            .background("create_maps2/slot/blue", 1, 1));
        add(0, 4, new Button("todo", 9, 1)
            .background("create_maps2/slot/blue", 1, 1));
        add(0, 5, new Button("todo", 9, 1)
            .background("create_maps2/slot/blue", 1, 1));
    }

    private void acceptNewMap(MapSlot slot) {
        // No need to re-sort, we know this should be first in the list.
        slots.addFirst(slot);
    }
}
