package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StoreView extends View {

    public static final int TAB_HYPERCUBE = 1;
    public static final int TAB_ADDONS = 2;

    private @Outlet("cubits_text") Text cubitsText;
    private @Outlet("hypercube_text") Text hypercubeText;
    private @Outlet("addons_text") Text addonsText;

    private @Outlet("name_switch") Switch nameSwitch;
    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_cubits_switch") Switch tabCubitsSwitch;
    private @Outlet("tab_hypercube_switch") Switch tabHypercubeSwitch;
    private @Outlet("tab_addons_switch") Switch tabAddOnsSwitch;

    private @OutletGroup("tab_id_.*") Label[] tabButtons;

    public StoreView(@NotNull Context context) {
        this(context, 0);
    }

    public StoreView(@NotNull Context context, int tab) {
        super(context);

        cubitsText.setText("Buy Cubits");
        hypercubeText.setText("Buy Hypercube");
        addonsText.setText("Buy Add-Ons");
        tabCubitsSwitch.setOption(1);

        for (int i = 0; i < tabButtons.length; i++) {
            var tabIndex = i;
            addActionHandler(
                    Objects.requireNonNull(tabButtons[tabIndex].id()),
                    Label.ActionHandler.lmb(_ -> selectTab(tabIndex))
            );
        }

        selectTab(tab);
    }

    private void selectTab(int index) {
        if (tabSwitch.getOption() == index) return;

        tabSwitch.setOption(index);
        nameSwitch.setOption(index);

        if (index == 0) {
            tabCubitsSwitch.setOption(1);
            tabHypercubeSwitch.setOption(0);
            tabAddOnsSwitch.setOption(0);
        } else if (index == 1) {
            tabCubitsSwitch.setOption(0);
            tabHypercubeSwitch.setOption(1);
            tabAddOnsSwitch.setOption(0);
        } else if (index == 2) {
            tabCubitsSwitch.setOption(0);
            tabHypercubeSwitch.setOption(0);
            tabAddOnsSwitch.setOption(1);
        }
    }
}
