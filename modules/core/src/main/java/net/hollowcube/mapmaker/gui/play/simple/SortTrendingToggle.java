package net.hollowcube.mapmaker.gui.play.simple;

import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class SortTrendingToggle extends AbstractToggle {
    public static final String SIG_TOGGLE = "sort_trending_toggle";

    public SortTrendingToggle(@NotNull Context context) {
        super(context, SIG_TOGGLE);
    }
}
