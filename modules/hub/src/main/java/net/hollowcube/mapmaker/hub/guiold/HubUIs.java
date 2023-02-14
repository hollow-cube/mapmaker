package net.hollowcube.mapmaker.hub.guiold;

import net.hollowcube.canvas.section.Section;
import net.hollowcube.mapmaker.hub.guiold.map.CreateMapsView;
import org.jetbrains.annotations.NotNull;

public final class HubUIs {
    private HubUIs() {}

    public static @NotNull Section createMaps() {
        return new CreateMapsView();
    }

}
