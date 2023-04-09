package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

public class MapEntry extends View {

    private final MapData map;

    public MapEntry(@NotNull Context context, @NotNull MapData map) {
        super(context);
        this.map = map;
    }

    @Action("btn")
    private void handleClick() {
        pushView(c -> new MapDetailsView(c, map));
    }

}
