package net.hollowcube.mapmaker.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class SortRecentToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "sort_recent_toggle";

    public SortRecentToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
