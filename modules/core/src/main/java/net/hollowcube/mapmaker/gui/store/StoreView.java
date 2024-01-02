package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StoreView extends View {

    private @Outlet("tab_switch") Switch tabSwitch;

    private @OutletGroup("tab_id_.*") Label[] tabButtons;

    public StoreView(@NotNull Context context) {
        super(context);

        for (int i = 0; i < tabButtons.length; i++) {
            var tabIndex = i;
            addActionHandler(
                    Objects.requireNonNull(tabButtons[i].id()),
                    Label.ActionHandler.lmb(player -> tabSwitch.setOption(tabIndex))
            );
        }
    }
}
