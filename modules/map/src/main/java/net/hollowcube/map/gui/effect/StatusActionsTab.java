package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class StatusActionsTab extends AbstractEffectActionsTab<StatusEffectData> {

    private @Outlet("add_time") Label addTimeLabel;

    public StatusActionsTab(@NotNull Context context) {
        super(context);
    }

    @Action("add_time")
    public void handleChangeAddTime() {
        pushView(context -> new BaseEffectTimeLimitAnvil(context, data.extraTime() > 0 ? String.valueOf(data.extraTime()) : ""));
    }

    @Signal(BaseEffectTimeLimitAnvil.SIG_UPDATE_NAME)
    public void handleUpdateTimeLimit(@NotNull String index) {
        if (index.isEmpty()) {
            data.setExtraTime(0);
            updateFromData();
            return;
        }

        try {
            var extraTime = Integer.parseInt(index);
            if (extraTime < 0 || extraTime >= 86_400) return;
            data.setExtraTime(extraTime);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }


    @Override
    protected void updateFromData() {
        super.updateFromData();

        addTimeLabel.setArgs(data.extraTime() <= 0 ? Component.translatable("gui.status.add_time.none") : Component.text(data.extraTime()));
    }
}
