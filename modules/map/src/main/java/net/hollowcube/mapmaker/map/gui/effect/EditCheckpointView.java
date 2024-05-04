package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import org.jetbrains.annotations.NotNull;

public class EditCheckpointView extends View {

    private @Outlet("tab_switch") Switch tabSwitch;
    private @Outlet("tab_settings_state_switch") Switch tabSettingsStateSwitch;
    private @Outlet("tab_actions_state_switch") Switch tabActionsStateSwitch;

    private @Outlet("settings") CheckpointSettingsTab settingsTab;
    private @Outlet("actions") CheckpointActionsTab actionsTab;

    private final CheckpointEffectData data;
    private final Runnable save;

    public EditCheckpointView(@NotNull Context context, @NotNull CheckpointEffectData data, int maxResetHeight, @NotNull Runnable save) {
        super(context);
        this.data = data;
        this.save = save;

        tabSettingsStateSwitch.setOption(true);
        settingsTab.setData(data, maxResetHeight);
        actionsTab.setData(data, save);
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
        if (this.save != null) this.save.run();
    }

}
