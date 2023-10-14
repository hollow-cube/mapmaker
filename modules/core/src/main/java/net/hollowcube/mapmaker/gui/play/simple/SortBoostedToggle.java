package net.hollowcube.mapmaker.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class SortBoostedToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "sort_boosted_toggle";

    public SortBoostedToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
