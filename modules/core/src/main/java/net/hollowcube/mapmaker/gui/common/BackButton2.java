package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackButton2 extends View {

    private static final int OPT_BACK = 0;
    private static final int OPT_CLOSE = 1;

    private @Outlet("switch") Switch typeSwitch;

    public BackButton2(@NotNull Context context) {
        super(context);
        typeSwitch.setOption(canPopView() ? OPT_BACK : OPT_CLOSE);
    }

    @Action("back2")
    public void handleBackClick(@NotNull Player player) {
        popView(); // Popview actually will close the inventory if there is no view to pop to
    }

    @Action("close2")
    public void handleCloseClick(@NotNull Player player) {
        popView(); // Popview actually will close the inventory if there is no view to pop to
    }
}
