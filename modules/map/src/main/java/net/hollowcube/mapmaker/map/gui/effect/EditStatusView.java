package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class EditStatusView extends View {

    private @Outlet("title") Text titleText;
    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_text") Text tabText;
    private @Outlet("tab_settings_state_switch") Switch tabSettingsStateSwitch;
    private @Outlet("tab_actions_state_switch") Switch tabActionsStateSwitch;

    private @Outlet("settings") StatusSettingsTab settingsTab;
    private @Outlet("actions") StatusActionsTab actionsTab;

    private final Runnable onClose;

    public EditStatusView(@NotNull Context context, @NotNull StatusEffectData data, int maxResetHeight, @NotNull Runnable onClose) {
        super(context);
        this.onClose = onClose;

        titleText.setText("Edit Status Plate");
        tabSettingsStateSwitch.setOption(true);
        tabText.setText("SP Settings");
        tabText.setArgs(Component.text("Status Plate Settings"));
        settingsTab.setData(data, maxResetHeight);
        actionsTab.setData(data, onClose);
    }

    @Action("tab_settings")
    public void handleSelectSettingsTab() {
        tabSwitch.setOption(0);
        tabText.setText("SP Settings");
        tabText.setArgs(Component.text("Status Plate Settings"));
        tabSettingsStateSwitch.setOption(true);
        tabActionsStateSwitch.setOption(false);
    }

    @Action("tab_actions")
    public void handleSelectActionsTab() {
        tabSwitch.setOption(1);
        tabText.setText("SP Actions");
        tabText.setArgs(Component.text("Status Plate Actions"));
        tabSettingsStateSwitch.setOption(false);
        tabActionsStateSwitch.setOption(true);
    }

    @Signal(Element.SIG_CLOSE)
    public void onClose() {
        if (this.onClose != null) this.onClose.run();
    }

}
