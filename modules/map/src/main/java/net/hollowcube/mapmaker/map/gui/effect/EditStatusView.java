package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import org.jetbrains.annotations.NotNull;

public class EditStatusView extends View {

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_settings_state_switch") Switch tabSettingsStateSwitch;
    private @Outlet("tab_actions_state_switch") Switch tabActionsStateSwitch;

    private @Outlet("settings") StatusSettingsTab settingsTab;
    private @Outlet("actions") StatusActionsTab actionsTab;

    private final Runnable onClose;

    public EditStatusView(@NotNull Context context, @NotNull StatusEffectData data, int maxResetHeight, @NotNull Runnable onClose) {
        super(context);
        this.onClose = onClose;

        tabSettingsStateSwitch.setOption(true);
        settingsTab.setData(data, maxResetHeight);
        actionsTab.setData(data, onClose);
    }

    @Action("tab_settings")
    public void handleSelectSettingsTab() {
        tabSwitch.setOption(0);
        tabSettingsStateSwitch.setOption(true);
        tabActionsStateSwitch.setOption(false);
    }

    @Action("tab_actions")
    public void handleSelectActionsTab() {
        tabSwitch.setOption(1);
        tabSettingsStateSwitch.setOption(false);
        tabActionsStateSwitch.setOption(true);
    }

    @Signal(Element.SIG_CLOSE)
    public void onClose() {
        if (this.onClose != null) this.onClose.run();
    }

}
