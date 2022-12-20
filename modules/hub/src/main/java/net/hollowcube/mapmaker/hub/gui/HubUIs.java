package net.hollowcube.mapmaker.hub.gui;

import net.hollowcube.canvas.Section;
import net.hollowcube.mapmaker.hub.gui.map.CreateMapsView;
import org.jetbrains.annotations.NotNull;

public final class HubUIs {
    private HubUIs() {}

    public static @NotNull Section createMaps() {
        return new CreateMapsView();
    }

}
