package net.hollowcube.mapmaker.hub.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class SortApprovedToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "sort_featured_toggle";

    public SortApprovedToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
