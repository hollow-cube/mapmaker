package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class PlayMaps extends View {

    private @Outlet("parkour_toggle") Label parkourToggle;

    public PlayMaps(@NotNull Context context) {
        super(context);
    }

    @Action("parkour_toggle")
    private void parkourToggle() {
        parkourToggle.setState(parkourToggle.getState() == State.ACTIVE ? State.HIDDEN : State.ACTIVE);
    }
}
