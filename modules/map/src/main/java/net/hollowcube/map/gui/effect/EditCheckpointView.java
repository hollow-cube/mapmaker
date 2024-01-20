package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.CheckpointEffectData;
import org.jetbrains.annotations.NotNull;

public class EditCheckpointView extends View {

    private @Outlet("tab_switch") Switch tabSwitch;

    private @Outlet("settings") CheckpointSettingsTab settingsTab;
    private @Outlet("actions") CheckpointActionsTab actionsTab;

    private final CheckpointEffectData data;
    private final Runnable onClose;

    public EditCheckpointView(@NotNull Context context, @NotNull CheckpointEffectData data, int maxResetHeight, @NotNull Runnable onClose) {
        super(context);
        this.data = data;
        this.onClose = onClose;

        settingsTab.setData(data, maxResetHeight);
        actionsTab.setData(data);
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
