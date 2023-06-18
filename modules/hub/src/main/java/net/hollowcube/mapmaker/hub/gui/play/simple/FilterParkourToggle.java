package net.hollowcube.mapmaker.hub.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class FilterParkourToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "filter_parkour_toggle";

    public FilterParkourToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
