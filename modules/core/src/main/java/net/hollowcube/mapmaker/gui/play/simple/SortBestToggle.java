package net.hollowcube.mapmaker.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class SortBestToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "sort_best_toggle";

    public SortBestToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
