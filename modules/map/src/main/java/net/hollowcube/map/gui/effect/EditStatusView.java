package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import org.jetbrains.annotations.NotNull;

public class EditStatusView extends View {

    private @Outlet("tab_switch") Switch tabSwitch;

    private @Outlet("settings") StatusSettingsTab settingsTab;
    private @Outlet("actions") StatusActionsTab actionsTab;

    private final Runnable onClose;

    public EditStatusView(@NotNull Context context, @NotNull StatusEffectData data, int maxResetHeight, @NotNull Runnable onClose) {
        super(context);
        this.onClose = onClose;

        settingsTab.setData(data, maxResetHeight);
        actionsTab.setData(data, onClose);
    }

    @Action("tab_settings")
    public void handleSelectSettingsTab() {
        tabSwitch.setOption(0);
    }

    @Action("tab_actions")
    public void handleSelectActionsTab() {
        tabSwitch.setOption(1);
    }

    @Signal(Element.SIG_CLOSE)
    public void onClose() {
        if (this.onClose != null) this.onClose.run();
    }

}
